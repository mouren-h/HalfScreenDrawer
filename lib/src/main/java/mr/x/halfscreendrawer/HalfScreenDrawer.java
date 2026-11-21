package mr.x.halfscreendrawer;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * 中半屏抽屉：默认 50% 屏高，按住横条上拉到 60%，下拉关闭。
 * 外壳（面板、横条、拖拽）全部 Java 代码创建，
 * 只有内容区加载调用方传入的 XML 布局（如 R.layout.home）。
 * 不依赖任何 values/drawable 资源文件。
 *
 * 用法：
 *   HalfScreenDrawer drawer = new HalfScreenDrawer(this, R.layout.home);
 *   drawer.setOnBindViewListener(new HalfScreenDrawer.OnBindViewListener() {
 *       @Override public void onBindView(View contentView) {
 *           // contentView 即 R.layout.home 加载出来的实例，可 findViewById 设置子控件
 *       }
 *   });
 *   drawer.gb(); // 手动关闭，带缓慢下滑动画
 */
public class HalfScreenDrawer {

    /** 回调：把加载好的内容布局实例交给调用方设置子控件 */
    public interface OnBindViewListener {
        void onBindView(View contentView);
    }

    private static final long ANIM_SHOW = 450; // 拉起显示动画时长（毫秒），想更慢就调大
    private static final long ANIM_HIDE = 400; // 关闭动画时长（毫秒），想更慢就调大

    private final Activity activity;
    private final int screenH;
    private final int baseH;   // 50% 屏高
    private final int maxH;    // 60% 屏高

    private Dialog dialog;
    private LinearLayout panel;
    private View contentView;
    private boolean closing = false; // 防止关闭动画被重复触发

    /** 静态创建方式，等价于构造方法 */
    public static HalfScreenDrawer show(Activity activity, int layoutResId) {
        return new HalfScreenDrawer(activity, layoutResId);
    }

    public HalfScreenDrawer(Activity activity, int layoutResId) {
        this.activity = activity;
        screenH = activity.getResources().getDisplayMetrics().heightPixels;
        baseH = (int) (screenH * 0.5f);
        maxH = (int) (screenH * 0.6f);
        try {
            final float d = activity.getResources().getDisplayMetrics().density;

            final Dialog dlg = new Dialog(activity);
            dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dlg.setCanceledOnTouchOutside(true);

            // ===== 外壳全部 Java 代码创建 =====
            final FrameLayout rootBox = new FrameLayout(activity);
            int p = (int) (20 * d + 0.5f);

            final LinearLayout pnl = new LinearLayout(activity);
            pnl.setOrientation(LinearLayout.VERTICAL);
            pnl.setPadding(p, (int) (10 * d), p, (int) (20 * d));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0xFFFFFFFF);
            bg.setCornerRadii(new float[]{22 * d, 22 * d, 22 * d, 22 * d, 0f, 0f, 0f, 0f});
            pnl.setBackground(bg);
            pnl.setClickable(true);

            // 拖拽横条
            FrameLayout handleBox = new FrameLayout(activity);
            pnl.addView(handleBox, new LinearLayout.LayoutParams(-1, (int) (26 * d)));
            View handle = new View(activity);
            GradientDrawable hb = new GradientDrawable();
            hb.setColor(0xFFD9DDE3);
            hb.setCornerRadius(3 * d);
            handle.setBackground(hb);
            FrameLayout.LayoutParams lpH = new FrameLayout.LayoutParams((int) (40 * d), (int) (5 * d));
            lpH.gravity = Gravity.CENTER;
            handleBox.addView(handle, lpH);

            // 内容容器：唯一从 XML 加载的部分
            FrameLayout contentBox = new FrameLayout(activity);
            pnl.addView(contentBox, new LinearLayout.LayoutParams(-1, 0, 1f));

            // ===== 加载调用方传入的 XML 内容布局 =====
            View content = LayoutInflater.from(activity).inflate(layoutResId, contentBox, false);
            contentBox.addView(content, new FrameLayout.LayoutParams(-1, -1));

            // 给示例内容控件补上 Java 创建的背景和点击（按 id 名查找，不存在就跳过）
            styleSampleContent(content, d);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, baseH);
            lp.gravity = Gravity.BOTTOM;
            rootBox.addView(pnl, lp);

            // 横条拖拽：上拉最高 60%，下拉超过 25% 缓慢下滑关闭，否则回弹吸附
            handleBox.setOnTouchListener(new View.OnTouchListener() {
                float downRaw = 0f;
                int startH = 0;

                @Override
                public boolean onTouch(View view, MotionEvent ev) {
                    int act = ev.getAction();
                    if (act == MotionEvent.ACTION_DOWN) {
                        downRaw = ev.getRawY();
                        startH = pnl.getHeight();
                        return true;
                    }
                    if (act == MotionEvent.ACTION_MOVE) {
                        float dy = ev.getRawY() - downRaw;
                        int nh = (int) (startH - dy);
                        float ty = 0f;
                        if (nh > maxH) {
                            nh = maxH;
                        }
                        if (nh < baseH) {
                            ty = baseH - nh;
                            nh = baseH;
                        }
                        ViewGroup.LayoutParams lpp = pnl.getLayoutParams();
                        lpp.height = nh;
                        pnl.setLayoutParams(lpp);
                        pnl.setTranslationY(ty);
                        return true;
                    }
                    if (act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_CANCEL) {
                        float ty = pnl.getTranslationY();
                        if (ty > baseH * 0.25f) {
                            // 下拉超过阈值：走统一的缓慢关闭动画
                            closeWithAnimation();
                        } else {
                            pnl.animate().translationY(0f).setDuration(150).start();
                            int target = baseH;
                            if (pnl.getHeight() > baseH + (maxH - baseH) / 2) {
                                target = maxH;
                            }
                            if (pnl.getHeight() != target) {
                                ValueAnimator va = ValueAnimator.ofInt(pnl.getHeight(), target);
                                va.setDuration(200);
                                va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                    @Override
                                    public void onAnimationUpdate(ValueAnimator animation) {
                                        int value = (Integer) animation.getAnimatedValue();
                                        ViewGroup.LayoutParams lp2 = pnl.getLayoutParams();
                                        lp2.height = value;
                                        pnl.setLayoutParams(lp2);
                                    }
                                });
                                va.start();
                            }
                        }
                        return true;
                    }
                    return false;
                }
            });

            // 点击面板以外的暗色区域：也走缓慢下滑关闭动画，不再瞬间消失
            rootBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    closeWithAnimation();
                }
            });
            // 拦截返回键/外部触摸触发的 cancel，同样走缓慢关闭动画
            dlg.setOnCancelListener(new android.content.DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(android.content.DialogInterface di) {
                    // cancel 时 Dialog 会自行消失，这里不做处理；
                    // 外部点击已由 rootBox 接管为动画关闭
                }
            });

            dlg.setContentView(rootBox, new ViewGroup.LayoutParams(-1, -1));
            Window win = dlg.getWindow();
            win.setBackgroundDrawable(new ColorDrawable(0));
            win.setLayout(-1, -1);
            win.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            win.setDimAmount(0.42f);
            dlg.show();

            // 入场动画：从底部缓缓滑入（减速曲线，先快后慢更自然）
            pnl.setTranslationY(baseH);
            pnl.animate()
                    .translationY(0f)
                    .setDuration(ANIM_SHOW)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();

            this.dialog = dlg;
            this.panel = pnl;
            this.contentView = content;
        } catch (Throwable t) {
            Toast.makeText(activity, "弹窗显示失败", Toast.LENGTH_SHORT).show();
        }
    }

    /** 按 id 名查找控件（不依赖编译期 R.id，换成你自己的布局也不会报错） */
    private View findByName(View root, String idName) {
        int id = activity.getResources().getIdentifier(idName, "id", activity.getPackageName());
        if (id == 0) {
            return null;
        }
        return root.findViewById(id);
    }

    /** 给示例内容的条目和收起按钮设置 Java 创建的背景与点击；控件不存在则跳过 */
    private void styleSampleContent(View content, float d) {
        String[] itemNames = {"tvItemA", "tvItemB", "tvItemC", "tvItemD"};
        for (String name : itemNames) {
            View item = findByName(content, name);
            if (item == null) {
                continue;
            }
            GradientDrawable ib = new GradientDrawable();
            ib.setColor(0xFFF2F4F8);
            ib.setCornerRadius(12 * d);
            GradientDrawable im = new GradientDrawable();
            im.setColor(0xFFFFFFFF);
            im.setCornerRadius(12 * d);
            item.setBackground(new RippleDrawable(ColorStateList.valueOf(0x1A1F2430), ib, im));
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    closeWithAnimation();
                    Toast.makeText(activity, "已选中半屏抽屉里的条目", Toast.LENGTH_SHORT).show();
                }
            });
        }
        View close = findByName(content, "btnClose");
        if (close != null) {
            GradientDrawable cb = new GradientDrawable();
            cb.setColor(0xFF1F2430);
            cb.setCornerRadius(14 * d);
            close.setBackground(cb);
            close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    closeWithAnimation();
                }
            });
        }
    }

    /** 回调方法：立即把加载好的内容布局实例交给你设置子控件 */
    public HalfScreenDrawer setOnBindViewListener(OnBindViewListener listener) {
        if (listener != null && contentView != null) {
            listener.onBindView(contentView);
        }
        return this;
    }

    /** 获取内容布局实例（R.layout.home 加载出来的 View） */
    public View getContentView() {
        return contentView;
    }

    /** 缓慢下滑关闭动画：面板缓缓滑出屏幕底部后再真正关闭 */
    public void closeWithAnimation() {
        if (panel == null || dialog == null || closing) {
            return;
        }
        closing = true;
        panel.animate()
                .translationY(screenH)
                .setDuration(ANIM_HIDE)
                .setInterpolator(new AccelerateInterpolator())
                .start();
        panel.postDelayed(new Runnable() {
            @Override
            public void run() {
                dismiss();
            }
        }, ANIM_HIDE - 10);
    }

    /** 直接关闭（无动画，一般用 gb() 代替） */
    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
