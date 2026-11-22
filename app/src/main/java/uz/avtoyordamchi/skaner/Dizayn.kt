package uz.avtoyordamchi.skaner

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/** Ranglar palitrasi: och va to'q mavzu. */
object R2 {
    var qorongi = false
        private set

    var FON1 = 0; var FON2 = 0; var KARTA = 0; var KARTA2 = 0; var CHEGARA = 0
    var OQ = 0; var MATN2 = 0; var XIRA = 0
    var KOK = 0; var FIRUZA = 0; var BINAFSHA = 0; var YASHIL = 0; var SARIQ = 0; var QIZIL = 0
    var URGU = 0; var TREK = 0; var TREK_CHIZIQ = 0; var MAYDON = 0

    init { mavzu(false) }

    fun mavzu(tom: Boolean) {
        qorongi = tom
        fun c(s: String) = Color.parseColor(s)
        if (tom) {
            FON1 = c("#060A14"); FON2 = c("#0E1730"); KARTA = c("#121C36"); KARTA2 = c("#18244A"); CHEGARA = c("#22305A")
            OQ = c("#F4F7FF"); MATN2 = c("#C9D3EA"); XIRA = c("#8391B3")
            KOK = c("#3D8BFF"); FIRUZA = c("#22D3EE"); BINAFSHA = c("#8B5CF6")
            YASHIL = c("#22C55E"); SARIQ = c("#F59E0B"); QIZIL = c("#EF4444")
            URGU = c("#1B2B5C"); TREK = c("#1B2747"); TREK_CHIZIQ = c("#2A3758"); MAYDON = c("#0A1022")
        } else {
            FON1 = c("#F3F5F8"); FON2 = c("#EEF1F5"); KARTA = c("#FFFFFF"); KARTA2 = c("#F5F7FA"); CHEGARA = c("#E5E8EE")
            OQ = c("#101828"); MATN2 = c("#344054"); XIRA = c("#8A94A6")
            KOK = c("#2563EB"); FIRUZA = c("#3B82F6"); BINAFSHA = c("#7C3AED")
            YASHIL = c("#16A34A"); SARIQ = c("#D97706"); QIZIL = c("#DC2626")
            URGU = c("#EAF1FF"); TREK = c("#E9EDF3"); TREK_CHIZIQ = c("#D3D9E3"); MAYDON = c("#F3F5F8")
        }
    }

    fun shaffof(rang: Int, alfa: Int) = Color.argb(alfa, Color.red(rang), Color.green(rang), Color.blue(rang))

    fun ballRangi(ball: Int) = when {
        ball >= 85 -> YASHIL
        ball >= 60 -> SARIQ
        else -> QIZIL
    }

    fun ballSozi(ball: Int) = when {
        ball >= 85 -> "a'lo holat"
        ball >= 60 -> "e'tibor bering"
        else -> "jiddiy"
    }
}

/** Shriftlar: Manrope (matn) va Orbitron (raqamlar). */
object Shrift {
    private val kesh = HashMap<String, Typeface>()

    private fun ol(ctx: Context, nom: String): Typeface = kesh.getOrPut(nom) {
        try { Typeface.createFromAsset(ctx.assets, "fonts/$nom.ttf") } catch (_: Exception) { Typeface.DEFAULT }
    }

    fun oddiy(ctx: Context) = ol(ctx, "manrope_regular")
    fun orta(ctx: Context) = ol(ctx, "manrope_semibold")
    fun qalin(ctx: Context) = ol(ctx, "manrope_extrabold")
    fun raqam(ctx: Context) = ol(ctx, "orbitron_bold")
}

/** Chiziqli ikonkalar: assets ichidagi icons papkasi, oq PNG, rangi kodda beriladi. */
object Ikonka {
    private val kesh = HashMap<String, Bitmap?>()

    fun ol(ctx: Context, nom: String): Bitmap? = kesh.getOrPut(nom) {
        try { ctx.assets.open("icons/$nom.png").use { BitmapFactory.decodeStream(it) } } catch (_: Exception) { null }
    }
}

/** Qayta ishlatiladigan UI elementlari. */
class Kit(private val ctx: Context) {

    fun dp(v: Int) = (v * ctx.resources.displayMetrics.density).toInt()
    fun dpf(v: Int) = v * ctx.resources.displayMetrics.density

    enum class Vazn { ODDIY, ORTA, QALIN, RAQAM }

    fun matn(m: String, size: Float, rang: Int = R2.OQ, vazn: Vazn = Vazn.ODDIY) = TextView(ctx).apply {
        text = m
        textSize = size
        setTextColor(rang)
        typeface = when (vazn) {
            Vazn.ODDIY -> Shrift.oddiy(ctx)
            Vazn.ORTA -> Shrift.orta(ctx)
            Vazn.QALIN -> Shrift.qalin(ctx)
            Vazn.RAQAM -> Shrift.raqam(ctx)
        }
        includeFontPadding = false
    }

    fun shakl(rang: Int, radius: Int, chegara: Int? = null): GradientDrawable = GradientDrawable().apply {
        setColor(rang)
        cornerRadius = dpf(radius)
        if (chegara != null) setStroke(dp(1), chegara)
    }

    fun gradient(ranglar: IntArray, radius: Int, yonalish: GradientDrawable.Orientation = GradientDrawable.Orientation.TL_BR,
                 chegara: Int? = null): GradientDrawable =
        GradientDrawable(yonalish, ranglar).apply {
            cornerRadius = dpf(radius)
            if (chegara != null) setStroke(dp(1), chegara)
        }

    /** Bosilganda to'lqin effekti. */
    fun bosiladigan(fon: Drawable, radius: Int): Drawable =
        RippleDrawable(ColorStateList.valueOf(R2.shaffof(R2.KOK, 50)), fon, shakl(Color.WHITE, radius))

    /** Chiziqli ikonka, berilgan rangga bo'yalgan. */
    fun ikonka(nom: String, olcham: Int, rang: Int = R2.MATN2): ImageView = ImageView(ctx).apply {
        setImageBitmap(Ikonka.ol(ctx, nom))
        setColorFilter(rang, PorterDuff.Mode.SRC_IN)
        scaleType = ImageView.ScaleType.FIT_CENTER
        layoutParams = LinearLayout.LayoutParams(dp(olcham), dp(olcham))
    }

    /** Rangli fon ustida ikonka (yumaloq kvadrat). */
    fun ikonkaDoira(nom: String, olcham: Int, rang: Int): View = LinearLayout(ctx).apply {
        gravity = Gravity.CENTER
        background = shakl(R2.shaffof(rang, if (R2.qorongi) 40 else 26), olcham * 30 / 100)
        addView(ImageView(ctx).apply {
            setImageBitmap(Ikonka.ol(ctx, nom))
            setColorFilter(rang, PorterDuff.Mode.SRC_IN)
        }, LinearLayout.LayoutParams(dp(olcham * 52 / 100), dp(olcham * 52 / 100)))
        layoutParams = LinearLayout.LayoutParams(dp(olcham), dp(olcham))
    }

    fun karta(padding: Int = 16, fon: Int = R2.KARTA, radius: Int = 22): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(padding), dp(padding), dp(padding), dp(padding))
        background = shakl(fon, radius, R2.CHEGARA)
    }

    fun ustun(): LinearLayout = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

    fun qator(markaz: Boolean = true): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        if (markaz) gravity = Gravity.CENTER_VERTICAL
    }

    fun lp(w: Int, h: Int, weight: Float = 0f) =
        if (weight > 0f) LinearLayout.LayoutParams(w, h, weight) else LinearLayout.LayoutParams(w, h)

    fun bosh(o: Int) = View(ctx).apply { layoutParams = LinearLayout.LayoutParams(dp(o), dp(o)) }

    fun bolim(m: String, oxiriga: View? = null): View {
        val q = qator()
        q.addView(matn(m, 18f, R2.OQ, Vazn.QALIN), lp(0, -2, 1f))
        if (oxiriga != null) q.addView(oxiriga)
        q.layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(26); bottomMargin = dp(12) }
        return q
    }

    /** Asosiy tugma. */
    fun katta(m: String, ikonkaNomi: String?, bosish: () -> Unit): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        val fon = gradient(intArrayOf(R2.KOK, R2.FIRUZA), 20, GradientDrawable.Orientation.LEFT_RIGHT)
        background = bosiladigan(fon, 20)
        elevation = dpf(if (R2.qorongi) 8 else 3)
        if (ikonkaNomi != null) addView(ikonka(ikonkaNomi, 22, Color.WHITE).apply {
            (layoutParams as LinearLayout.LayoutParams).marginEnd = dp(10)
        })
        addView(matn(m, 16.5f, Color.WHITE, Vazn.QALIN))
        isClickable = true
        setOnClickListener { bosish() }
    }

    /** Ikkinchi darajali tugma. */
    fun tugma(m: String, ikonkaNomi: String?, rang: Int = R2.OQ, bosish: () -> Unit): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setPadding(dp(14), 0, dp(14), 0)
        background = bosiladigan(shakl(R2.KARTA, 16, R2.CHEGARA), 16)
        if (ikonkaNomi != null) addView(ikonka(ikonkaNomi, 19, if (rang == R2.OQ) R2.KOK else rang).apply {
            (layoutParams as LinearLayout.LayoutParams).marginEnd = dp(8)
        })
        addView(matn(m, 14f, rang, Vazn.ORTA))
        isClickable = true
        setOnClickListener { bosish() }
    }

    /** Tez amal plitkasi: rangli fonda ikonka + nom. */
    fun amal(ikonkaNomi: String, nom: String, rang: Int = R2.KOK, bosish: () -> Unit): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(12), dp(16))
        background = bosiladigan(shakl(R2.KARTA, 22, R2.CHEGARA), 22)
        addView(ikonkaDoira(ikonkaNomi, 44, rang))
        addView(matn(nom, 14f, R2.OQ, Vazn.QALIN).apply { setPadding(0, dp(12), 0, 0) })
        isClickable = true
        setOnClickListener { bosish() }
    }

    /** Ko'rsatkich plitkasi: (plitka, qiymat matni). */
    fun plitka(ikonkaNomi: String?, nom: String, qiymat: String, rang: Int = R2.OQ): Pair<LinearLayout, TextView> {
        val k = karta(14, R2.KARTA, 20)
        val tepa = qator()
        if (ikonkaNomi != null) tepa.addView(ikonka(ikonkaNomi, 18, R2.XIRA).apply {
            (layoutParams as LinearLayout.LayoutParams).marginEnd = dp(8)
        })
        tepa.addView(matn(nom, 12f, R2.XIRA, Vazn.ORTA).apply { maxLines = 2 }, lp(0, -2, 1f))
        k.addView(tepa)
        val v = matn(qiymat, 17f, rang, Vazn.QALIN).apply { setPadding(0, dp(10), 0, 0) }
        k.addView(v)
        return k to v
    }

    fun panjara(elementlar: List<View>, ustunlar: Int = 2, oraliq: Int = 10): LinearLayout {
        val p = ustun()
        elementlar.chunked(ustunlar).forEachIndexed { i, guruh ->
            val q = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
            for (j in 0 until ustunlar) {
                val v = if (j < guruh.size) guruh[j] else View(ctx)
                q.addView(v, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                    if (j > 0) marginStart = dp(oraliq)
                })
            }
            p.addView(q, LinearLayout.LayoutParams(-1, -2).apply { if (i > 0) topMargin = dp(oraliq) })
        }
        return p
    }

    fun chip(m: String, rang: Int, size: Float = 11.5f): TextView = matn(m, size, rang, Vazn.QALIN).apply {
        setPadding(dp(10), dp(5), dp(10), dp(5))
        background = shakl(R2.shaffof(rang, if (R2.qorongi) 36 else 22), 12, R2.shaffof(rang, if (R2.qorongi) 90 else 60))
    }

    fun ajratgich(): View = View(ctx).apply {
        setBackgroundColor(R2.CHEGARA)
        layoutParams = LinearLayout.LayoutParams(-1, dp(1))
    }

    /** "Nom ........ qiymat" qatori. */
    fun juft(nom: String, qiymat: String, rang: Int = R2.OQ): LinearLayout = qator().apply {
        setPadding(0, dp(11), 0, dp(11))
        addView(matn(nom, 13.5f, R2.XIRA, Vazn.ORTA), lp(0, -2, 1f))
        addView(matn(qiymat, 13.5f, rang, Vazn.QALIN).apply { gravity = Gravity.END }, lp(0, -2, 1.3f))
    }

    fun maydon(qiymat: String, ishora: String, raqam: Boolean = false): EditText = EditText(ctx).apply {
        setText(qiymat)
        hint = ishora
        inputType = if (raqam) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        textSize = 15.5f
        typeface = Shrift.orta(ctx)
        setTextColor(R2.OQ)
        setHintTextColor(R2.XIRA)
        background = shakl(R2.MAYDON, 14, R2.CHEGARA)
        setPadding(dp(14), dp(12), dp(14), dp(12))
        setSingleLine(true)
    }

    /** Sarlavhali maydon bloki. */
    fun maydonBlok(nom: String, v: View): LinearLayout = ustun().apply {
        addView(matn(nom, 12f, R2.XIRA, Vazn.ORTA).apply { setPadding(dp(2), 0, 0, dp(6)) })
        addView(v, LinearLayout.LayoutParams(-1, -2))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
    }

    /** Tanlanadigan qator (bosilganda ro'yxat ochiladi). */
    fun tanlash(qiymat: String, bosish: () -> Unit): TextView = matn(qiymat, 15.5f, R2.OQ, Vazn.ORTA).apply {
        background = bosiladigan(shakl(R2.MAYDON, 14, R2.CHEGARA), 14)
        setPadding(dp(14), dp(13), dp(14), dp(13))
        isClickable = true
        setOnClickListener { bosish() }
    }

    /** Variantli almashtirgich. */
    fun segment(variantlar: List<String>, tanlangan: Int, tanlov: (Int) -> Unit): LinearLayout {
        val q = qator().apply {
            background = shakl(R2.MAYDON, 14, R2.CHEGARA)
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        val tugmalar = mutableListOf<TextView>()
        fun belgila(i: Int) {
            tugmalar.forEachIndexed { j, t ->
                if (j == i) {
                    t.background = shakl(R2.KOK, 11)
                    t.setTextColor(Color.WHITE)
                } else {
                    t.background = null
                    t.setTextColor(R2.XIRA)
                }
            }
        }
        variantlar.forEachIndexed { i, v ->
            val t = matn(v, 14f, R2.XIRA, Vazn.QALIN).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(10), 0, dp(10))
                setOnClickListener { belgila(i); tanlov(i) }
            }
            tugmalar.add(t)
            q.addView(t, lp(0, -2, 1f))
        }
        belgila(tanlangan)
        return q
    }
}
