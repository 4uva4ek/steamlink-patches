.class final Lcn/vr4p/patch/SubtitlePositionStrings;
.super Ljava/lang/Object;
.source "SubtitlePositionStrings.java"


# instance fields
.field final down:Ljava/lang/String;

.field final language:Ljava/lang/String;

.field final reset:Ljava/lang/String;

.field final resetDescription:Ljava/lang/String;

.field final title:Ljava/lang/String;

.field final up:Ljava/lang/String;


# direct methods
.method private constructor <init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
    .locals 0

    .line 14
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 15
    iput-object p1, p0, Lcn/vr4p/patch/SubtitlePositionStrings;->language:Ljava/lang/String;

    .line 16
    iput-object p2, p0, Lcn/vr4p/patch/SubtitlePositionStrings;->title:Ljava/lang/String;

    .line 17
    iput-object p3, p0, Lcn/vr4p/patch/SubtitlePositionStrings;->reset:Ljava/lang/String;

    .line 18
    iput-object p4, p0, Lcn/vr4p/patch/SubtitlePositionStrings;->down:Ljava/lang/String;

    .line 19
    iput-object p5, p0, Lcn/vr4p/patch/SubtitlePositionStrings;->up:Ljava/lang/String;

    .line 20
    iput-object p6, p0, Lcn/vr4p/patch/SubtitlePositionStrings;->resetDescription:Ljava/lang/String;

    .line 21
    return-void
.end method

.method private static english()Lcn/vr4p/patch/SubtitlePositionStrings;
    .locals 8

    .line 64
    new-instance v7, Lcn/vr4p/patch/SubtitlePositionStrings;

    const-string v5, "Move subtitles up"

    const-string v6, "Reset subtitle position"

    const-string v1, "en"

    const-string v2, "Subtitle position"

    const-string v3, "Reset"

    const-string v4, "Move subtitles down"

    move-object v0, v7

    invoke-direct/range {v0 .. v6}, Lcn/vr4p/patch/SubtitlePositionStrings;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V

    return-object v7
.end method

.method static varargs forLocales([Ljava/util/Locale;)Lcn/vr4p/patch/SubtitlePositionStrings;
    .locals 17

    .line 24
    move-object/from16 v0, p0

    array-length v1, v0

    const/4 v2, 0x0

    move v3, v2

    :goto_0
    if-ge v3, v1, :cond_5

    aget-object v4, v0, v3

    .line 25
    if-nez v4, :cond_0

    goto/16 :goto_6

    .line 26
    :cond_0
    invoke-virtual {v4}, Ljava/util/Locale;->getLanguage()Ljava/lang/String;

    move-result-object v5

    invoke-virtual {v5}, Ljava/lang/String;->hashCode()I

    move-result v6

    const/4 v7, 0x1

    sparse-switch v6, :sswitch_data_0

    :cond_1
    goto :goto_1

    :sswitch_0
    const-string v6, "zh"

    invoke-virtual {v5, v6}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v5

    if-eqz v5, :cond_1

    const/4 v5, 0x2

    goto :goto_2

    :sswitch_1
    const-string v6, "ru"

    invoke-virtual {v5, v6}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v5

    if-eqz v5, :cond_1

    const/4 v5, 0x7

    goto :goto_2

    :sswitch_2
    const-string v6, "ko"

    invoke-virtual {v5, v6}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v5

    if-eqz v5, :cond_1

    move v5, v2

    goto :goto_2

    :sswitch_3
    const-string v6, "ja"

    invoke-virtual {v5, v6}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v5

    if-eqz v5, :cond_1

    move v5, v7

    goto :goto_2

    :sswitch_4
    const-string v6, "it"

    invoke-virtual {v5, v6}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v5

    if-eqz v5, :cond_1

    const/4 v5, 0x6

    goto :goto_2

    :sswitch_5
    const-string v6, "fr"

    invoke-virtual {v5, v6}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v5

    if-eqz v5, :cond_1

    const/4 v5, 0x4

    goto :goto_2

    :sswitch_6
    const-string v6, "es"

    invoke-virtual {v5, v6}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v5

    if-eqz v5, :cond_1

    const/4 v5, 0x5

    goto :goto_2

    :sswitch_7
    const-string v6, "en"

    invoke-virtual {v5, v6}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v5

    if-eqz v5, :cond_1

    const/16 v5, 0x8

    goto :goto_2

    :sswitch_8
    const-string v6, "de"

    invoke-virtual {v5, v6}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v5

    if-eqz v5, :cond_1

    const/4 v5, 0x3

    goto :goto_2

    :goto_1
    const/4 v5, -0x1

    :goto_2
    packed-switch v5, :pswitch_data_0

    goto/16 :goto_6

    .line 56
    :pswitch_0
    invoke-static {}, Lcn/vr4p/patch/SubtitlePositionStrings;->english()Lcn/vr4p/patch/SubtitlePositionStrings;

    move-result-object v0

    return-object v0

    .line 53
    :pswitch_1
    new-instance v0, Lcn/vr4p/patch/SubtitlePositionStrings;

    const-string v6, "\u041f\u0435\u0440\u0435\u043c\u0435\u0441\u0442\u0438\u0442\u044c \u0441\u0443\u0431\u0442\u0438\u0442\u0440\u044b \u0432\u0432\u0435\u0440\u0445"

    const-string v7, "\u0421\u0431\u0440\u043e\u0441\u0438\u0442\u044c \u043f\u043e\u043b\u043e\u0436\u0435\u043d\u0438\u0435 \u0441\u0443\u0431\u0442\u0438\u0442\u0440\u043e\u0432"

    const-string v2, "ru"

    const-string v3, "\u041f\u043e\u043b\u043e\u0436\u0435\u043d\u0438\u0435 \u0441\u0443\u0431\u0442\u0438\u0442\u0440\u043e\u0432"

    const-string v4, "\u0421\u0431\u0440\u043e\u0441"

    const-string v5, "\u041f\u0435\u0440\u0435\u043c\u0435\u0441\u0442\u0438\u0442\u044c \u0441\u0443\u0431\u0442\u0438\u0442\u0440\u044b \u0432\u043d\u0438\u0437"

    move-object v1, v0

    invoke-direct/range {v1 .. v7}, Lcn/vr4p/patch/SubtitlePositionStrings;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V

    return-object v0

    .line 50
    :pswitch_2
    new-instance v0, Lcn/vr4p/patch/SubtitlePositionStrings;

    const-string v13, "Sposta i sottotitoli in alto"

    const-string v14, "Ripristina la posizione dei sottotitoli"

    const-string v9, "it"

    const-string v10, "Posizione dei sottotitoli"

    const-string v11, "Ripristina"

    const-string v12, "Sposta i sottotitoli in basso"

    move-object v8, v0

    invoke-direct/range {v8 .. v14}, Lcn/vr4p/patch/SubtitlePositionStrings;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V

    return-object v0

    .line 47
    :pswitch_3
    new-instance v0, Lcn/vr4p/patch/SubtitlePositionStrings;

    const-string v6, "Mover los subt\u00edtulos hacia arriba"

    const-string v7, "Restablecer la posici\u00f3n de los subt\u00edtulos"

    const-string v2, "es"

    const-string v3, "Posici\u00f3n de los subt\u00edtulos"

    const-string v4, "Restablecer"

    const-string v5, "Mover los subt\u00edtulos hacia abajo"

    move-object v1, v0

    invoke-direct/range {v1 .. v7}, Lcn/vr4p/patch/SubtitlePositionStrings;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V

    return-object v0

    .line 44
    :pswitch_4
    new-instance v0, Lcn/vr4p/patch/SubtitlePositionStrings;

    const-string v13, "D\u00e9placer les sous-titres vers le haut"

    const-string v14, "R\u00e9initialiser la position des sous-titres"

    const-string v9, "fr"

    const-string v10, "Position des sous-titres"

    const-string v11, "R\u00e9initialiser"

    const-string v12, "D\u00e9placer les sous-titres vers le bas"

    move-object v8, v0

    invoke-direct/range {v8 .. v14}, Lcn/vr4p/patch/SubtitlePositionStrings;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V

    return-object v0

    .line 41
    :pswitch_5
    new-instance v0, Lcn/vr4p/patch/SubtitlePositionStrings;

    const-string v6, "Untertitel nach oben verschieben"

    const-string v7, "Untertitelposition zur\u00fccksetzen"

    const-string v2, "de"

    const-string v3, "Untertitelposition"

    const-string v4, "Zur\u00fccksetzen"

    const-string v5, "Untertitel nach unten verschieben"

    move-object v1, v0

    invoke-direct/range {v1 .. v7}, Lcn/vr4p/patch/SubtitlePositionStrings;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V

    return-object v0

    .line 32
    :pswitch_6
    invoke-virtual {v4}, Ljava/util/Locale;->getScript()Ljava/lang/String;

    move-result-object v0

    .line 33
    invoke-virtual {v4}, Ljava/util/Locale;->getCountry()Ljava/lang/String;

    move-result-object v1

    .line 34
    const-string v3, "Hant"

    invoke-virtual {v3, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-nez v3, :cond_3

    invoke-virtual {v0}, Ljava/lang/String;->isEmpty()Z

    move-result v0

    if-eqz v0, :cond_2

    .line 35
    const-string v0, "TW"

    invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_3

    const-string v0, "HK"

    invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_3

    const-string v0, "MO"

    invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_2

    goto :goto_3

    :cond_2
    goto :goto_4

    :cond_3
    :goto_3
    move v2, v7

    .line 36
    :goto_4
    if-eqz v2, :cond_4

    .line 37
    new-instance v0, Lcn/vr4p/patch/SubtitlePositionStrings;

    const-string v8, "\u5411\u4e0a\u79fb\u52d5\u5b57\u5e55"

    const-string v9, "\u91cd\u7f6e\u5b57\u5e55\u4f4d\u7f6e"

    const-string v4, "zh-Hant"

    const-string v5, "\u5b57\u5e55\u4f4d\u7f6e"

    const-string v6, "\u91cd\u7f6e"

    const-string v7, "\u5411\u4e0b\u79fb\u52d5\u5b57\u5e55"

    move-object v3, v0

    invoke-direct/range {v3 .. v9}, Lcn/vr4p/patch/SubtitlePositionStrings;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V

    goto :goto_5

    .line 39
    :cond_4
    new-instance v0, Lcn/vr4p/patch/SubtitlePositionStrings;

    const-string v15, "\u5411\u4e0a\u79fb\u52a8\u5b57\u5e55"

    const-string v16, "\u91cd\u7f6e\u5b57\u5e55\u4f4d\u7f6e"

    const-string v11, "zh-Hans"

    const-string v12, "\u5b57\u5e55\u4f4d\u7f6e"

    const-string v13, "\u91cd\u7f6e"

    const-string v14, "\u5411\u4e0b\u79fb\u52a8\u5b57\u5e55"

    move-object v10, v0

    invoke-direct/range {v10 .. v16}, Lcn/vr4p/patch/SubtitlePositionStrings;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V

    .line 36
    :goto_5
    return-object v0

    .line 29
    :pswitch_7
    new-instance v0, Lcn/vr4p/patch/SubtitlePositionStrings;

    const-string v6, "\u5b57\u5e55\u3092\u4e0a\u306b\u79fb\u52d5"

    const-string v7, "\u5b57\u5e55\u306e\u4f4d\u7f6e\u3092\u30ea\u30bb\u30c3\u30c8"

    const-string v2, "ja"

    const-string v3, "\u5b57\u5e55\u306e\u4f4d\u7f6e"

    const-string v4, "\u30ea\u30bb\u30c3\u30c8"

    const-string v5, "\u5b57\u5e55\u3092\u4e0b\u306b\u79fb\u52d5"

    move-object v1, v0

    invoke-direct/range {v1 .. v7}, Lcn/vr4p/patch/SubtitlePositionStrings;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V

    return-object v0

    .line 27
    :pswitch_8
    new-instance v0, Lcn/vr4p/patch/SubtitlePositionStrings;

    const-string v13, "\uc790\ub9c9 \uc704\ub85c \uc774\ub3d9"

    const-string v14, "\uc790\ub9c9 \uc704\uce58 \ucd08\uae30\ud654"

    const-string v9, "ko"

    const-string v10, "\uc790\ub9c9 \uc704\uce58"

    const-string v11, "\ucd08\uae30\ud654"

    const-string v12, "\uc790\ub9c9 \uc544\ub798\ub85c \uc774\ub3d9"

    move-object v8, v0

    invoke-direct/range {v8 .. v14}, Lcn/vr4p/patch/SubtitlePositionStrings;-><init>(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V

    return-object v0

    .line 24
    :goto_6
    add-int/lit8 v3, v3, 0x1

    goto/16 :goto_0

    .line 60
    :cond_5
    invoke-static {}, Lcn/vr4p/patch/SubtitlePositionStrings;->english()Lcn/vr4p/patch/SubtitlePositionStrings;

    move-result-object v0

    return-object v0

    nop

    :sswitch_data_0
    .sparse-switch
        0xc81 -> :sswitch_8
        0xca9 -> :sswitch_7
        0xcae -> :sswitch_6
        0xccc -> :sswitch_5
        0xd2b -> :sswitch_4
        0xd37 -> :sswitch_3
        0xd64 -> :sswitch_2
        0xe43 -> :sswitch_1
        0xf2e -> :sswitch_0
    .end sparse-switch

    :pswitch_data_0
    .packed-switch 0x0
        :pswitch_8
        :pswitch_7
        :pswitch_6
        :pswitch_5
        :pswitch_4
        :pswitch_3
        :pswitch_2
        :pswitch_1
        :pswitch_0
    .end packed-switch
.end method
