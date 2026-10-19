.class public final Lcn/vr4p/patch/GxrToastContinuity;
.super Ljava/lang/Object;
.source "GxrToastContinuity.java"

# interfaces
.implements Landroid/app/Application$ActivityLifecycleCallbacks;


# static fields
.field private static final DISPLAY_MS:J = 0x12cL

.field private static final PERIOD_MS:J = 0xdacL

.field private static final TAG:Ljava/lang/String; = "4XVRMemoryPulse"

.field private static instance:Lcn/vr4p/patch/GxrToastContinuity;


# instance fields
.field private active:Landroid/app/Activity;

.field private final check:Ljava/lang/Runnable;

.field private disabled:Z

.field private failures:I

.field private generation:I

.field private final handler:Landroid/os/Handler;

.field private hide:Ljava/lang/Runnable;

.field private final next:Ljava/lang/Runnable;

.field private nextScheduled:Z

.field private nextShowAt:J

.field private final power:Landroid/os/PowerManager;

.field private toast:Landroid/widget/Toast;


# direct methods
.method public static synthetic $r8$lambda$bgoLRKYs4886-F07yQJRe_bM_-8(Lcn/vr4p/patch/GxrToastContinuity;)V
    .locals 0

    invoke-direct {p0}, Lcn/vr4p/patch/GxrToastContinuity;->show()V

    return-void
.end method

.method public static synthetic $r8$lambda$lnIeCiWCVNFWfcOaEhHONyWWWK0(Lcn/vr4p/patch/GxrToastContinuity;)V
    .locals 0

    invoke-direct {p0}, Lcn/vr4p/patch/GxrToastContinuity;->check()V

    return-void
.end method

.method private constructor <init>(Landroid/app/Application;)V
    .locals 2

    .line 33
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 22
    new-instance v0, Landroid/os/Handler;

    invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;

    move-result-object v1

    invoke-direct {v0, v1}, Landroid/os/Handler;-><init>(Landroid/os/Looper;)V

    iput-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->handler:Landroid/os/Handler;

    .line 30
    new-instance v0, Lcn/vr4p/patch/GxrToastContinuity$$ExternalSyntheticLambda1;

    invoke-direct {v0, p0}, Lcn/vr4p/patch/GxrToastContinuity$$ExternalSyntheticLambda1;-><init>(Lcn/vr4p/patch/GxrToastContinuity;)V

    iput-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->next:Ljava/lang/Runnable;

    .line 31
    new-instance v0, Lcn/vr4p/patch/GxrToastContinuity$$ExternalSyntheticLambda2;

    invoke-direct {v0, p0}, Lcn/vr4p/patch/GxrToastContinuity$$ExternalSyntheticLambda2;-><init>(Lcn/vr4p/patch/GxrToastContinuity;)V

    iput-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->check:Ljava/lang/Runnable;

    .line 33
    const-class v0, Landroid/os/PowerManager;

    invoke-virtual {p1, v0}, Landroid/app/Application;->getSystemService(Ljava/lang/Class;)Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Landroid/os/PowerManager;

    iput-object p1, p0, Lcn/vr4p/patch/GxrToastContinuity;->power:Landroid/os/PowerManager;

    return-void
.end method

.method static synthetic access$000(Lcn/vr4p/patch/GxrToastContinuity;)I
    .locals 0

    .line 18
    iget p0, p0, Lcn/vr4p/patch/GxrToastContinuity;->generation:I

    return p0
.end method

.method static synthetic access$008(Lcn/vr4p/patch/GxrToastContinuity;)I
    .locals 2

    .line 18
    iget v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->generation:I

    add-int/lit8 v1, v0, 0x1

    iput v1, p0, Lcn/vr4p/patch/GxrToastContinuity;->generation:I

    return v0
.end method

.method static synthetic access$100(Lcn/vr4p/patch/GxrToastContinuity;)Landroid/widget/Toast;
    .locals 0

    .line 18
    iget-object p0, p0, Lcn/vr4p/patch/GxrToastContinuity;->toast:Landroid/widget/Toast;

    return-object p0
.end method

.method static synthetic access$102(Lcn/vr4p/patch/GxrToastContinuity;Landroid/widget/Toast;)Landroid/widget/Toast;
    .locals 0

    .line 18
    iput-object p1, p0, Lcn/vr4p/patch/GxrToastContinuity;->toast:Landroid/widget/Toast;

    return-object p1
.end method

.method static synthetic access$200(Lcn/vr4p/patch/GxrToastContinuity;)Z
    .locals 0

    .line 18
    invoke-direct {p0}, Lcn/vr4p/patch/GxrToastContinuity;->ready()Z

    move-result p0

    return p0
.end method

.method static synthetic access$300(Lcn/vr4p/patch/GxrToastContinuity;)V
    .locals 0

    .line 18
    invoke-direct {p0}, Lcn/vr4p/patch/GxrToastContinuity;->release()V

    return-void
.end method

.method static synthetic access$402(Lcn/vr4p/patch/GxrToastContinuity;I)I
    .locals 0

    .line 18
    iput p1, p0, Lcn/vr4p/patch/GxrToastContinuity;->failures:I

    return p1
.end method

.method static synthetic access$500(Lcn/vr4p/patch/GxrToastContinuity;)Ljava/lang/Runnable;
    .locals 0

    .line 18
    iget-object p0, p0, Lcn/vr4p/patch/GxrToastContinuity;->hide:Ljava/lang/Runnable;

    return-object p0
.end method

.method static synthetic access$502(Lcn/vr4p/patch/GxrToastContinuity;Ljava/lang/Runnable;)Ljava/lang/Runnable;
    .locals 0

    .line 18
    iput-object p1, p0, Lcn/vr4p/patch/GxrToastContinuity;->hide:Ljava/lang/Runnable;

    return-object p1
.end method

.method static synthetic access$600(Lcn/vr4p/patch/GxrToastContinuity;)Landroid/os/Handler;
    .locals 0

    .line 18
    iget-object p0, p0, Lcn/vr4p/patch/GxrToastContinuity;->handler:Landroid/os/Handler;

    return-object p0
.end method

.method static synthetic access$700(Lcn/vr4p/patch/GxrToastContinuity;J)V
    .locals 0

    .line 18
    invoke-direct {p0, p1, p2}, Lcn/vr4p/patch/GxrToastContinuity;->schedule(J)V

    return-void
.end method

.method private cancel(Landroid/widget/Toast;)V
    .locals 2

    .line 112
    :try_start_0
    invoke-virtual {p1}, Landroid/widget/Toast;->cancel()V
    :try_end_0
    .catch Ljava/lang/RuntimeException; {:try_start_0 .. :try_end_0} :catch_0

    .line 119
    goto :goto_0

    .line 113
    :catch_0
    move-exception p1

    .line 114
    const/4 v0, 0x1

    iput-boolean v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->disabled:Z

    .line 115
    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->handler:Landroid/os/Handler;

    iget-object v1, p0, Lcn/vr4p/patch/GxrToastContinuity;->check:Ljava/lang/Runnable;

    invoke-virtual {v0, v1}, Landroid/os/Handler;->removeCallbacks(Ljava/lang/Runnable;)V

    .line 116
    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->handler:Landroid/os/Handler;

    iget-object v1, p0, Lcn/vr4p/patch/GxrToastContinuity;->next:Ljava/lang/Runnable;

    invoke-virtual {v0, v1}, Landroid/os/Handler;->removeCallbacks(Ljava/lang/Runnable;)V

    .line 117
    const/4 v0, 0x0

    iput-boolean v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->nextScheduled:Z

    .line 118
    const-string v0, "4XVRMemoryPulse"

    const-string v1, "Recovery toast could not be cancelled; helper disabled"

    invoke-static {v0, v1, p1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    .line 120
    :goto_0
    return-void
.end method

.method private check()V
    .locals 4

    .line 135
    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->active:Landroid/app/Activity;

    if-eqz v0, :cond_5

    iget-boolean v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->disabled:Z

    if-eqz v0, :cond_0

    goto :goto_2

    .line 136
    :cond_0
    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->active:Landroid/app/Activity;

    invoke-virtual {v0}, Landroid/app/Activity;->isFinishing()Z

    move-result v0

    if-nez v0, :cond_4

    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->active:Landroid/app/Activity;

    invoke-virtual {v0}, Landroid/app/Activity;->isDestroyed()Z

    move-result v0

    if-eqz v0, :cond_1

    goto :goto_1

    .line 137
    :cond_1
    invoke-direct {p0}, Lcn/vr4p/patch/GxrToastContinuity;->ready()Z

    move-result v0

    if-eqz v0, :cond_2

    const-wide/16 v0, 0x0

    invoke-direct {p0, v0, v1}, Lcn/vr4p/patch/GxrToastContinuity;->schedule(J)V

    goto :goto_0

    .line 138
    :cond_2
    invoke-direct {p0}, Lcn/vr4p/patch/GxrToastContinuity;->release()V

    .line 139
    :goto_0
    iget-boolean v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->disabled:Z

    if-nez v0, :cond_3

    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->handler:Landroid/os/Handler;

    iget-object v1, p0, Lcn/vr4p/patch/GxrToastContinuity;->check:Ljava/lang/Runnable;

    const-wide/16 v2, 0x7d0

    invoke-virtual {v0, v1, v2, v3}, Landroid/os/Handler;->postDelayed(Ljava/lang/Runnable;J)Z

    .line 140
    :cond_3
    return-void

    .line 136
    :cond_4
    :goto_1
    invoke-direct {p0}, Lcn/vr4p/patch/GxrToastContinuity;->stop()V

    return-void

    .line 135
    :cond_5
    :goto_2
    return-void
.end method

.method public static installForActivity(Landroid/app/Activity;)V
    .locals 2

    .line 36
    invoke-static {}, Landroid/os/Looper;->myLooper()Landroid/os/Looper;

    move-result-object v0

    invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;

    move-result-object v1

    if-ne v0, v1, :cond_2

    .line 37
    sget-object v0, Lcn/vr4p/patch/GxrToastContinuity;->instance:Lcn/vr4p/patch/GxrToastContinuity;

    if-nez v0, :cond_1

    const-string v0, "samsung"

    sget-object v1, Landroid/os/Build;->MANUFACTURER:Ljava/lang/String;

    invoke-virtual {v0, v1}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_1

    sget-object v0, Landroid/os/Build;->DEVICE:Ljava/lang/String;

    .line 38
    const-string v1, "xrvst2"

    invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_1

    const-string v0, "I610UEU2AZF3"

    sget-object v1, Landroid/os/Build$VERSION;->INCREMENTAL:Ljava/lang/String;

    invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_0

    goto :goto_0

    .line 39
    :cond_0
    invoke-virtual {p0}, Landroid/app/Activity;->getApplication()Landroid/app/Application;

    move-result-object p0

    .line 40
    new-instance v0, Lcn/vr4p/patch/GxrToastContinuity;

    invoke-direct {v0, p0}, Lcn/vr4p/patch/GxrToastContinuity;-><init>(Landroid/app/Application;)V

    sput-object v0, Lcn/vr4p/patch/GxrToastContinuity;->instance:Lcn/vr4p/patch/GxrToastContinuity;

    .line 41
    sget-object v0, Lcn/vr4p/patch/GxrToastContinuity;->instance:Lcn/vr4p/patch/GxrToastContinuity;

    invoke-virtual {p0, v0}, Landroid/app/Application;->registerActivityLifecycleCallbacks(Landroid/app/Application$ActivityLifecycleCallbacks;)V

    .line 42
    const-string p0, "4XVRMemoryPulse"

    const-string v0, "Memory pulse helper registered"

    invoke-static {p0, v0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    .line 43
    return-void

    .line 38
    :cond_1
    :goto_0
    return-void

    .line 36
    :cond_2
    new-instance p0, Ljava/lang/IllegalStateException;

    const-string v0, "Install on main thread"

    invoke-direct {p0, v0}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw p0
.end method

.method private ready()Z
    .locals 1

    .line 46
    iget-boolean v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->disabled:Z

    if-nez v0, :cond_0

    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->active:Landroid/app/Activity;

    if-eqz v0, :cond_0

    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->active:Landroid/app/Activity;

    invoke-virtual {v0}, Landroid/app/Activity;->isFinishing()Z

    move-result v0

    if-nez v0, :cond_0

    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->active:Landroid/app/Activity;

    invoke-virtual {v0}, Landroid/app/Activity;->isDestroyed()Z

    move-result v0

    if-nez v0, :cond_0

    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->power:Landroid/os/PowerManager;

    if-eqz v0, :cond_0

    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->power:Landroid/os/PowerManager;

    .line 47
    invoke-virtual {v0}, Landroid/os/PowerManager;->isInteractive()Z

    move-result v0

    if-eqz v0, :cond_0

    const/4 v0, 0x1

    goto :goto_0

    :cond_0
    const/4 v0, 0x0

    .line 46
    :goto_0
    return v0
.end method

.method private release()V
    .locals 2

    .line 123
    iget v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->generation:I

    add-int/lit8 v0, v0, 0x1

    iput v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->generation:I

    .line 124
    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->handler:Landroid/os/Handler;

    iget-object v1, p0, Lcn/vr4p/patch/GxrToastContinuity;->next:Ljava/lang/Runnable;

    invoke-virtual {v0, v1}, Landroid/os/Handler;->removeCallbacks(Ljava/lang/Runnable;)V

    .line 125
    const/4 v0, 0x0

    iput-boolean v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->nextScheduled:Z

    .line 126
    const-wide/16 v0, 0x0

    iput-wide v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->nextShowAt:J

    .line 127
    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->hide:Ljava/lang/Runnable;

    if-eqz v0, :cond_0

    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->handler:Landroid/os/Handler;

    iget-object v1, p0, Lcn/vr4p/patch/GxrToastContinuity;->hide:Ljava/lang/Runnable;

    invoke-virtual {v0, v1}, Landroid/os/Handler;->removeCallbacks(Ljava/lang/Runnable;)V

    .line 128
    :cond_0
    const/4 v0, 0x0

    iput-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->hide:Ljava/lang/Runnable;

    .line 129
    iget-object v1, p0, Lcn/vr4p/patch/GxrToastContinuity;->toast:Landroid/widget/Toast;

    .line 130
    iput-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->toast:Landroid/widget/Toast;

    .line 131
    if-eqz v1, :cond_1

    invoke-direct {p0, v1}, Lcn/vr4p/patch/GxrToastContinuity;->cancel(Landroid/widget/Toast;)V

    .line 132
    :cond_1
    return-void
.end method

.method private schedule(J)V
    .locals 6

    .line 51
    invoke-direct {p0}, Lcn/vr4p/patch/GxrToastContinuity;->ready()Z

    move-result v0

    if-eqz v0, :cond_1

    iget v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->failures:I

    const/4 v1, 0x3

    if-ge v0, v1, :cond_1

    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->toast:Landroid/widget/Toast;

    if-nez v0, :cond_1

    iget-boolean v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->nextScheduled:Z

    if-eqz v0, :cond_0

    goto :goto_0

    .line 52
    :cond_0
    const/4 v0, 0x1

    iput-boolean v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->nextScheduled:Z

    .line 53
    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->handler:Landroid/os/Handler;

    iget-object v1, p0, Lcn/vr4p/patch/GxrToastContinuity;->next:Ljava/lang/Runnable;

    iget-wide v2, p0, Lcn/vr4p/patch/GxrToastContinuity;->nextShowAt:J

    invoke-static {}, Landroid/os/SystemClock;->uptimeMillis()J

    move-result-wide v4

    sub-long/2addr v2, v4

    invoke-static {p1, p2, v2, v3}, Ljava/lang/Math;->max(JJ)J

    move-result-wide p1

    invoke-virtual {v0, v1, p1, p2}, Landroid/os/Handler;->postDelayed(Ljava/lang/Runnable;J)Z

    .line 54
    return-void

    .line 51
    :cond_1
    :goto_0
    return-void
.end method

.method private show()V
    .locals 9

    .line 57
    const/4 v0, 0x0

    iput-boolean v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->nextScheduled:Z

    .line 58
    invoke-direct {p0}, Lcn/vr4p/patch/GxrToastContinuity;->ready()Z

    move-result v1

    if-eqz v1, :cond_1

    iget v1, p0, Lcn/vr4p/patch/GxrToastContinuity;->failures:I

    const/4 v2, 0x3

    if-ge v1, v2, :cond_1

    iget-object v1, p0, Lcn/vr4p/patch/GxrToastContinuity;->toast:Landroid/widget/Toast;

    if-eqz v1, :cond_0

    goto :goto_1

    .line 59
    :cond_0
    iget v1, p0, Lcn/vr4p/patch/GxrToastContinuity;->generation:I

    const/4 v2, 0x1

    add-int/2addr v1, v2

    iput v1, p0, Lcn/vr4p/patch/GxrToastContinuity;->generation:I

    .line 61
    :try_start_0
    new-instance v3, Lcn/vr4p/patch/GxrToastContinuity$1;

    iget-object v4, p0, Lcn/vr4p/patch/GxrToastContinuity;->active:Landroid/app/Activity;

    invoke-direct {v3, p0, v4}, Lcn/vr4p/patch/GxrToastContinuity$1;-><init>(Lcn/vr4p/patch/GxrToastContinuity;Landroid/content/Context;)V

    .line 64
    invoke-virtual {v3, v0}, Landroid/view/View;->setBackgroundColor(I)V

    .line 65
    const/4 v4, 0x4

    invoke-virtual {v3, v4}, Landroid/view/View;->setImportantForAccessibility(I)V

    .line 66
    new-instance v4, Landroid/widget/Toast;

    iget-object v5, p0, Lcn/vr4p/patch/GxrToastContinuity;->active:Landroid/app/Activity;

    invoke-direct {v4, v5}, Landroid/widget/Toast;-><init>(Landroid/content/Context;)V

    .line 67
    iput-object v4, p0, Lcn/vr4p/patch/GxrToastContinuity;->toast:Landroid/widget/Toast;

    .line 68
    invoke-virtual {v4, v2}, Landroid/widget/Toast;->setDuration(I)V

    .line 69
    const/16 v5, 0x33

    invoke-virtual {v4, v5, v0, v0}, Landroid/widget/Toast;->setGravity(III)V

    .line 70
    invoke-virtual {v4, v3}, Landroid/widget/Toast;->setView(Landroid/view/View;)V

    .line 71
    new-instance v0, Lcn/vr4p/patch/GxrToastContinuity$2;

    invoke-direct {v0, p0, v1, v4}, Lcn/vr4p/patch/GxrToastContinuity$2;-><init>(Lcn/vr4p/patch/GxrToastContinuity;ILandroid/widget/Toast;)V

    invoke-virtual {v4, v0}, Landroid/widget/Toast;->addCallback(Landroid/widget/Toast$Callback;)V

    .line 90
    invoke-static {}, Landroid/os/SystemClock;->uptimeMillis()J

    move-result-wide v5

    .line 91
    const-wide/16 v7, 0xdac

    add-long/2addr v7, v5

    iput-wide v7, p0, Lcn/vr4p/patch/GxrToastContinuity;->nextShowAt:J

    .line 92
    invoke-virtual {v4}, Landroid/widget/Toast;->show()V

    .line 93
    new-instance v0, Lcn/vr4p/patch/GxrToastContinuity$$ExternalSyntheticLambda0;

    invoke-direct {v0, p0, v1, v4}, Lcn/vr4p/patch/GxrToastContinuity$$ExternalSyntheticLambda0;-><init>(Lcn/vr4p/patch/GxrToastContinuity;ILandroid/widget/Toast;)V

    iput-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->hide:Ljava/lang/Runnable;

    .line 103
    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->handler:Landroid/os/Handler;

    iget-object v1, p0, Lcn/vr4p/patch/GxrToastContinuity;->hide:Ljava/lang/Runnable;

    const-wide/16 v3, 0x12c

    add-long/2addr v5, v3

    invoke-static {}, Landroid/os/SystemClock;->uptimeMillis()J

    move-result-wide v3

    sub-long/2addr v5, v3

    const-wide/16 v3, 0x0

    invoke-static {v3, v4, v5, v6}, Ljava/lang/Math;->max(JJ)J

    move-result-wide v3

    invoke-virtual {v0, v1, v3, v4}, Landroid/os/Handler;->postDelayed(Ljava/lang/Runnable;J)Z
    :try_end_0
    .catch Ljava/lang/RuntimeException; {:try_start_0 .. :try_end_0} :catch_0

    .line 108
    goto :goto_0

    .line 104
    :catch_0
    move-exception v0

    .line 105
    iget v1, p0, Lcn/vr4p/patch/GxrToastContinuity;->failures:I

    add-int/2addr v1, v2

    iput v1, p0, Lcn/vr4p/patch/GxrToastContinuity;->failures:I

    .line 106
    invoke-direct {p0}, Lcn/vr4p/patch/GxrToastContinuity;->release()V

    .line 107
    const-string v1, "4XVRMemoryPulse"

    const-string v2, "Recovery toast could not be shown"

    invoke-static {v1, v2, v0}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    .line 109
    :goto_0
    return-void

    .line 58
    :cond_1
    :goto_1
    return-void
.end method

.method private stop()V
    .locals 2

    .line 143
    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->handler:Landroid/os/Handler;

    iget-object v1, p0, Lcn/vr4p/patch/GxrToastContinuity;->check:Ljava/lang/Runnable;

    invoke-virtual {v0, v1}, Landroid/os/Handler;->removeCallbacks(Ljava/lang/Runnable;)V

    .line 144
    const/4 v0, 0x0

    iput-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->active:Landroid/app/Activity;

    .line 145
    invoke-direct {p0}, Lcn/vr4p/patch/GxrToastContinuity;->release()V

    .line 146
    return-void
.end method


# virtual methods
.method synthetic lambda$show$0$cn-vr4p-patch-GxrToastContinuity(ILandroid/widget/Toast;)V
    .locals 1

    .line 94
    iget v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->generation:I

    if-ne p1, v0, :cond_1

    iget-object p1, p0, Lcn/vr4p/patch/GxrToastContinuity;->toast:Landroid/widget/Toast;

    if-eq p1, p2, :cond_0

    goto :goto_0

    .line 97
    :cond_0
    iget p1, p0, Lcn/vr4p/patch/GxrToastContinuity;->generation:I

    add-int/lit8 p1, p1, 0x1

    iput p1, p0, Lcn/vr4p/patch/GxrToastContinuity;->generation:I

    .line 98
    const/4 p1, 0x0

    iput-object p1, p0, Lcn/vr4p/patch/GxrToastContinuity;->toast:Landroid/widget/Toast;

    .line 99
    iput-object p1, p0, Lcn/vr4p/patch/GxrToastContinuity;->hide:Ljava/lang/Runnable;

    .line 100
    invoke-direct {p0, p2}, Lcn/vr4p/patch/GxrToastContinuity;->cancel(Landroid/widget/Toast;)V

    .line 101
    const-wide/16 p1, 0x0

    invoke-direct {p0, p1, p2}, Lcn/vr4p/patch/GxrToastContinuity;->schedule(J)V

    .line 102
    return-void

    .line 94
    :cond_1
    :goto_0
    return-void
.end method

.method public onActivityCreated(Landroid/app/Activity;Landroid/os/Bundle;)V
    .locals 0

    .line 159
    return-void
.end method

.method public onActivityDestroyed(Landroid/app/Activity;)V
    .locals 1

    .line 158
    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->active:Landroid/app/Activity;

    if-ne v0, p1, :cond_0

    invoke-direct {p0}, Lcn/vr4p/patch/GxrToastContinuity;->stop()V

    :cond_0
    return-void
.end method

.method public onActivityPaused(Landroid/app/Activity;)V
    .locals 1

    .line 156
    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->active:Landroid/app/Activity;

    if-ne v0, p1, :cond_0

    invoke-direct {p0}, Lcn/vr4p/patch/GxrToastContinuity;->stop()V

    :cond_0
    return-void
.end method

.method public onActivityResumed(Landroid/app/Activity;)V
    .locals 3

    .line 149
    invoke-virtual {p1}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/Class;->getName()Ljava/lang/String;

    move-result-object v0

    const-string v1, "cn.vr4p.oculus4xvrplayerov.Main4XActivity"

    invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_2

    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->active:Landroid/app/Activity;

    if-ne v0, p1, :cond_0

    goto :goto_0

    .line 150
    :cond_0
    invoke-direct {p0}, Lcn/vr4p/patch/GxrToastContinuity;->stop()V

    .line 151
    iget-boolean v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->disabled:Z

    if-eqz v0, :cond_1

    return-void

    .line 152
    :cond_1
    iput-object p1, p0, Lcn/vr4p/patch/GxrToastContinuity;->active:Landroid/app/Activity;

    .line 153
    const/4 p1, 0x0

    iput p1, p0, Lcn/vr4p/patch/GxrToastContinuity;->failures:I

    .line 154
    iget-object p1, p0, Lcn/vr4p/patch/GxrToastContinuity;->handler:Landroid/os/Handler;

    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->check:Ljava/lang/Runnable;

    const-wide/16 v1, 0x2ee

    invoke-virtual {p1, v0, v1, v2}, Landroid/os/Handler;->postDelayed(Ljava/lang/Runnable;J)Z

    .line 155
    return-void

    .line 149
    :cond_2
    :goto_0
    return-void
.end method

.method public onActivitySaveInstanceState(Landroid/app/Activity;Landroid/os/Bundle;)V
    .locals 0

    .line 161
    return-void
.end method

.method public onActivityStarted(Landroid/app/Activity;)V
    .locals 0

    .line 160
    return-void
.end method

.method public onActivityStopped(Landroid/app/Activity;)V
    .locals 1

    .line 157
    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity;->active:Landroid/app/Activity;

    if-ne v0, p1, :cond_0

    invoke-direct {p0}, Lcn/vr4p/patch/GxrToastContinuity;->stop()V

    :cond_0
    return-void
.end method
