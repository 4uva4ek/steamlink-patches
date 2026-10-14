.class public final Lcn/vr4p/patch/PopupRefresh;
.super Ljava/lang/Object;
.source "PopupRefresh.java"


# direct methods
.method private constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static onHostResume(Lcn/vr4p/oculus4xvrplayerov/Main4XActivity;)V
    .locals 5

    :try_start_0
    invoke-static {}, Landroid/os/Environment;->isExternalStorageManager()Z

    move-result v0

    if-eqz v0, :cond_perm

    const-class v1, Lcn/vr4p/oculus4xvrplayerov/Main4XActivity;

    const-string v2, "m_PermissionsUI"

    invoke-virtual {v1, v2}, Ljava/lang/Class;->getDeclaredField(Ljava/lang/String;)Ljava/lang/reflect/Field;

    move-result-object v1

    const/4 v2, 0x1

    invoke-virtual {v1, v2}, Ljava/lang/reflect/Field;->setAccessible(Z)V

    invoke-virtual {v1, p0}, Ljava/lang/reflect/Field;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v1

    if-eqz v1, :cond_perm

    check-cast v1, Lcn/vr4p/oculus4xvrplayerov/V4Window;

    invoke-virtual {v1}, Lcn/vr4p/oculus4xvrplayerov/V4Window;->IsVisible()Z

    move-result v2

    if-eqz v2, :cond_perm

    invoke-virtual {v1}, Lcn/vr4p/oculus4xvrplayerov/V4Window;->SetHide()V

    :cond_perm
    invoke-virtual {p0}, Lcn/vr4p/oculus4xvrplayerov/Main4XActivity;->GetV4VersionUpdateDlg()Lcn/vr4p/oculus4xvrplayerov/Dialog/V4VersionUpdateDlg;

    move-result-object v1

    if-eqz v1, :cond_end

    invoke-virtual {v1}, Lcn/vr4p/oculus4xvrplayerov/V4Window;->IsVisible()Z

    move-result v2

    if-eqz v2, :cond_end

    const-string v2, "4XVersionSave"

    const/4 v3, 0x0

    invoke-virtual {p0, v2, v3}, Lcn/vr4p/oculus4xvrplayerov/Main4XActivity;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;

    move-result-object v2

    const-string v4, "Version"

    invoke-interface {v2, v4, v3}, Landroid/content/SharedPreferences;->getInt(Ljava/lang/String;I)I

    move-result v2

    invoke-virtual {p0}, Lcn/vr4p/oculus4xvrplayerov/Main4XActivity;->getPackageManager()Landroid/content/pm/PackageManager;

    move-result-object v3

    invoke-virtual {p0}, Lcn/vr4p/oculus4xvrplayerov/Main4XActivity;->getPackageName()Ljava/lang/String;

    move-result-object v4

    const/4 v0, 0x0

    invoke-virtual {v3, v4, v0}, Landroid/content/pm/PackageManager;->getPackageInfo(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;

    move-result-object v0

    iget v0, v0, Landroid/content/pm/PackageInfo;->versionCode:I

    div-int/lit8 v0, v0, 0x64

    div-int/lit8 v2, v2, 0x64

    if-lt v2, v0, :cond_end

    invoke-virtual {v1}, Lcn/vr4p/oculus4xvrplayerov/V4Window;->SetHide()V

    :cond_end
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0

    return-void

    :catch_0
    move-exception v0

    const-string v1, "4XVRPopupRefresh"

    const-string v2, "Popup refresh failed"

    invoke-static {v1, v2, v0}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    return-void
.end method
