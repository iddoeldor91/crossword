package com.ie.services;


import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.ie.models.Word;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.json.JsonParserFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CsvWordBankService implements WordBankService {

    private static final Logger LOG = LoggerFactory.getLogger(CsvWordBankService.class);

    private static final String FILE_DELIMITER = ",";

    private List<String> wordBankList;
    private String srcFilePath;
    private final int WORD_COLUMN = 0, OCCURRENCES_COLUMN = 1;

    private LoadingCache<String, String> cache;

    private void init() {
        //
        cache = CacheBuilder.newBuilder()
                .recordStats()
                .maximumSize(100)
                .recordStats()
                .build(new CacheLoader<String, String>() {
                    @Override
                    public String load(String key) throws Exception {
                        return getClue(key);
                    }
                });

        wordBankList = new LinkedList<>();
        String content;
//        TODO http://memorynotfound.com/load-file-resources-folder-java/
        try {
            content = IOUtils.toString(getClass().getResourceAsStream(srcFilePath), "UTF-8");
            LOG.info("words loaded from resource file successfully");
        } catch (IOException e) {
            LOG.error("words failed, getting fallback words" + e.getMessage());
            content = "של, 523419|את, 362815|על, 280167|הוא, 155755|לא, 122291|עם, 96065|זה, 87401|גם, 84639|או, 78755|היא, 77191|היה, 75562|בשנת, 64838|ב-, 63413|בין, 60246|כל, 58962|כי, 55162|לאחר, 52214|יותר, 51433|יש, 50630|ידי, 50232|אך, 48523|זו, 45694|אם, 44756|עד, 40754|ה-, 40047|אשר, 38185|הם, 38138|אני, 36317|הייתה, 36062|כמו, 35521|כך, 33862|אחד, 32299|שם, 32255|ביותר, 31724|היו, 31311|ישראל, 31235|רק, 30236|אותו, 28778|מספר, 27840|כאשר, 27744|בו, 27376|אל, 26473|זאת, 26325|מה, 26194|הערך, 26175|רבים, 25179|כדי, 24909|לו, 23670|אין, 23502|הראשון, 23494|הברית, 22746|שלו, 22503|אף, 22353|בית, 22179|ניתן, 21671|ולא, 21619|כן, 21034|שני, 21009|העיר, 19699|אבל, 19696|בן, 19415|שלא, 19289|בכל, 19123|העולם, 18829|בה, 18714|באופן, 18367|להיות, 18264|מן, 17990|אחת, 17930|שהוא, 17637|לי, 17458|לפני, 17197|אלו, 17171|חלק, 16675|שנים, 16465|במהלך, 16362|ערך, 16047|האם, 16028|לפי, 16018|בעיקר, 15935|בשם, 15788|פי, 15777|הראשונה, 15655|מאוד, 15615|כיום, 15615|אותה, 15531|אלא, 15289|עוד, 15168|כלל, 14951|דבר, 14896|אחרים, 14646|רבות, 14478|שנה, 14382|החל, 14363|שונים, 14126|כמה, 13971|אחר, 13851|מלחמת, 13814|הן, 13658|הספר, 13592|אלה, 13469|נולד, 13294|המדינה, 13262|כ-, 13261|שיחה, 13228|אולם, 13169|בעקבות, 13114|המאה, 13006|רב, 12978|אדם, 12978|השני, 12779|אינו, 12743|למרות, 12578|יכול, 12322|כבר, 12280|במקום, 12213|בני, 12178|וכן, 12164|לכל, 12133|אז, 12109|אחרי, 12019|היום, 12009|מכן, 11982|בערך, 11972|הזה, 11859|ללא, 11844|בעל, 11809|זמן, 11790|עצמו, 11688|שהיה, 11661|והוא, 11543|בשל, 11308|השנייה, 11265|בדרך, 11229|מנת, 11220|לה, 11191|בישראל, 11151|בהם, 10988|ראש, 10964|למשל, 10925|שנות, 10828|ארצות, 10790|דרך, 10735|רוב, 10691|באמצעות, 10643|תחת, 10639|ל-, 10572|כגון, 10557|כאן, 10463|בעולם, 10417|אפשר, 10375|שיש, 10337|צריך, 10285|אותם, 10241|שבו, 10166|השם, 10133|חיים, 10125|הרב, 10108|בעלי, 9981|דוד, 9853|מי, 9762|יום, 9726|ולכן, 9701|שלה, 9582|השנים, 9570|נגד, 9555|בלבד, 9496|יחד, 9459|בנוסף, 9417|שתי, 9416|ספר, 9404|בעיר, 9347|ואף, 9314|לכך, 9218|שונות, 9159|בבית, 9122|ו-, 9068|להם, 9067|בארץ, 8953|במשך, 8950|גדול, 8926|המלחמה, 8827|האדם, 8826|בקרב, 8769|כתב, 8750|הרבה, 8737|אנשים, 8729|שמו, 8719|תוך, 8717|זכה, 8683|באותה, 8661|לעתים, 8653|שימוש, 8630|ערכים, 8619|עבור, 8606|כפי, 8589|במרץ, 8527|וגם, 8512|ואת, 8481|מקום, 8441|מ-, 8441|דף, 8406|עליו, 8388|מתוך, 8314|בידי, 8311|נוספים, 8307|ועל, 8285|הלהקה, 8253|נראה, 8246|הארץ, 8197|ועוד, 8178|רבה, 8160|המשפט, 8147|מערכת, 8114|באוקטובר, 8057|הממשלה, 8024|בצורה, 8016|באזור, 7987|בספטמבר, 7929|הסרט, 7919|ממנו, 7881|הגדול, 7870|נוסף, 7867|בזמן, 7826|במיוחד, 7822|מידע, 7803|בדצמבר, 7781|מאז, 7750|בינואר, 7679|לבין, 7659|אביב, 7650|בנובמבר, 7587|אינה, 7562|בשנות, 7555|רבי, 7552|בעת, 7550|יהיה, 7532|לערך, 7526|בארצות, 7523|שנת, 7504|כלומר, 7496|בשנים, 7482|שלהם, 7476|עדיין, 7475|כמעט, 7459|למעשה, 7449|שהיא, 7321|היהודים, 7316|החברה, 7274|בפברואר, 7200|נכון, 7137|יהודים, 7102|נמצא, 7003|המלך, 6997|עבר, 6986|בגלל, 6905|שבה, 6892|הינו, 6873|לגבי, 6862|מהם, 6862|בכך, 6857|אחרות, 6828|הקבוצה, 6821|ועד, 6778|חברת, 6770|העברית, 6755|בסוף, 6754|לרוב, 6742|שלום, 6739|בתוך, 6708|כולל, 6640|לכתוב, 6614|באוגוסט, 6574|ראו, 6574|באפריל, 6560|יצא, 6553|משום, 6553|לראשונה, 6532|איש, 6460|במסגרת, 6437|בתחום, 6431|אנשי, 6394|טוב, 6373|באנגלית, 6367|הצבא, 6364|כנגד, 6363|שהם, 6358|גרמניה, 6338|מעל, 6296|האלבום, 6275|אצל, 6264|עקב, 6231|לעשות, 6188|הים, 6169|פעם, 6165|יולי, 6163|יוני, 6104|האי, 6073|הפך, 6070|במאי, 6062|לכן, 6060|חשוב, 6056|ביניהם, 6034|שכן, 6032|יהודי, 5997|בדף, 5985|לדוגמה, 5975|בעוד, 5940|ידוע, 5935|מדינות, 5911|מכיוון, 5910|לב, 5908|כלי, 5900|לאור, 5895|לשם, 5876|בתחילת, 5855|הזמן, 5854|מאוחר, 5831|במאה, 5819|החיים, 5811|אחרת, 5801|שוב, 5787|נחשב, 5780|אולי, 5778|משנת, 5770|דה, 5768|יהודה, 5763|למצוא, 5696|שלושה, 5696|מחדש, 5667|קיבל, 5652|תקופה, 5649|חדש, 5649|צרפת, 5647|בלתי, 5647|שהיו, 5641|הגיע, 5635|מאי, 5633|אינם, 5632|בנושא, 5631|החלה, 5622|שונה, 5608|מקור, 5576|לפנה\"ס, 5572|מול, 5562|ירושלים, 5556|לראות, 5525|למה, 5525|חבר, 5521|והיא, 5507|אירופה, 5502|קיים, 5499|קשר, 5453|לדעתי, 5450|מישהו, 5444|בתקופה, 5430|שאין, 5426|גדולה, 5410|ככל, 5408|נקרא, 5407|בתקופת, 5390|אי, 5388|המפלגה, 5384|נוספת, 5383|ימים, 5381|מותו, 5363|הכנסת, 5343|קשה, 5337|פחות, 5311|חברי, 5288|שלי, 5285|השנה, 5281|השלישי, 5269|ולאחר, 5255|תל, 5253|הבריטי, 5252|האחרון, 5247|קצר, 5242|דומה, 5241|קבוצת, 5239|מדובר, 5230|ליצור, 5206|אליו, 5196|איך, 5178|מדי, 5177|לאורך, 5145|ישנם, 5118|תוכנית, 5117|מבין, 5101|כוחות, 5101|הרי, 5082|פעמים, 5080|קטן, 5073|פעולה, 5064|היישוב, 5056|ביולי, 5051|האימפריה, 5041|הראשונים, 5022|בעבר, 4989|השיחה, 4985|על-ידי, 4972|השיר, 4957|כזה, 4956|אביו, 4953|להוסיף, 4932|העבודה, 4915|נעשה, 4914|ילדים, 4913|בניגוד, 4905|כיוון, 4898|מלך, 4880|האוויר, 4874|ויקיפדיה, 4872|מעבר, 4871|מכל, 4871|להשתמש, 4867|משתמש, 4867|כוח, 4862|משה, 4861|בירושלים, 4858|מצד, 4857|בנו, 4857|שלוש, 4846|ברור, 4836|ביוני, 4831|החדשה, 4822|הגדולה, 4812|בגיל, 4806|וכך, 4806|לקבל, 4799|חוק, 4798|לעומת, 4796|חיל, 4796|פני, 4794|פרס, 4787|מרץ, 4783|במלחמת, 4782|יחסית, 4779|צה\"ל, 4779|מדינת, 4776|כתוצאה, 4761|שהייתה, 4754|המועצות, 4752|מונה, 4725|קישור, 4703|שי, 4691|אתה, 4682|והן, 4678|החלו, 4678|תמונה, 4671|מים, 4644|בעברית, 4615|הראשי, 4609|ברית, 4596|השימוש, 4591|עצמה, 4586|בת, 4586|יודע, 4577|שאני, 4564|בתי, 4554|יוסף, 4541|בספר, 4530|במקרה, 4517|המילה, 4516|האנגלית, 4497|למד, 4494|בפני, 4487|היהודי, 4484|מסוים, 4475|יורק, 4475|המשחק, 4470|השלטון, 4462|זהו, 4449|העם, 4448|באותו, 4445|לצד, 4444|פרק, 4431|בראש, 4417|ארץ, 4409|מעט, 4408|זכתה, 4408|עבודה, 4407|זכויות, 4405|הערכים, 4404|בעזרת, 4401|שימש, 4393|מהווה, 4393|קודם, 4388|ואילו, 4373|הדבר, 4369|החדש, 4352|אפילו, 4346|התנועה, 4333|לאחד, 4332|לשנות, 4320|הדיון, 4318|נבחר, 4307|מרכז, 4306|תמיד, 4306|משפט, 4298|עיר, 4296|צורך, 4288|להגיע, 4275|--, 4272|בסיס, 4272|פשוט, 4268|בהן, 4242|תודה, 4238|פה, 4230|צבא, 4223|מופיע, 4218|קבוצות, 4185|ראשי, 4182|משחק, 4180|העליון, 4179|המידע, 4169|במספר, 4166|ביום, 4163|יצחק, 4160|היהודית, 4157|שזה, 4153|נוספות, 4140|ראשון, 4139|הישראלי, 4139|הינה, 4138|מיליון, 4129|הצליח, 4124|בהתאם, 4113|נשים, 4090|תקופת, 4085|הנושא, 4082|בסרט, 4066|והיה, 4064|לפחות, 4059|ברחבי, 4048|יכולים, 4046|שבהם, 4037|לארץ, 4028|קבוצה, 4011|הבית, 4003|משתמשים, 3991|ג'ון, 3988|ארגון, 3965|שמה, 3954|ליד, 3942|קיימת, 3940|בכלל, 3934|חושב, 3927|הקישור, 3911|מבחינה, 3906|תהליך, 3884|תפקיד, 3875|בצפון, 3861|ימי, 3848|ישירות, 3845|מחוץ, 3843|חדשים, 3842|מצב, 3830|כנראה, 3829|הנשיא, 3829|בויקיפדיה, 3827|הזו, 3810|סוף, 3808|עליה, 3802|ממש, 3785|כתוב, 3779|תנועת, 3767|באירופה, 3766|לציין, 3765|הפכה, 3761|יעקב, 3755|שר, 3755|ויש, 3754|כלפי, 3745|לכם, 3743|פעילות, 3741|שכל, 3720|שינוי, 3712|ממשלת, 3712|עלה, 3704|שירים, 3696|בהמשך, 3694|לדף, 3677|קרב, 3674|משהו, 3658|בדרום, 3657|חייו, 3657|והם, 3654|אזור, 3647|ובין, 3638|מדינה, 3632|מאשר, 3614|מפני, 3613|גבוהה, 3609|העתיקה, 3607|הארגון, 3606|בתור, 3600|קיימים, 3597|התקופה, 3594|לערוך, 3592|בשנה, 3586|כדאי, 3584|הבא, 3581|סרט, 3579|משמש, 3577|באוניברסיטת, 3576|הגיעו, 3572|מאות, 3569|חודשים, 3565|בלי, 3563|שום, 3555|מכך, 3547|האחרונות, 3544|ניו, 3539|שמות, 3536|הנראה, 3534|הללו, 3524|בעלת, 3521|בימי, 3520|שלמה, 3518|חדשה, 3510|עליהם, 3506|המים, 3498|אכן, 3490|יכולה, 3480|כללי, 3478|וכו', 3473|המקום, 3473|דווקא, 3472|דברים, 3462|לחלוטין, 3462|רוצה, 3462|התוכנית, 3454|במידה, 3453|סוג, 3449|שניתן, 3447|לבית, 3443|מלחמה, 3433|הקרב, 3430|השאר, 3427|בזכות, 3426|ספרים, 3425|אוגוסט, 3425|אתם, 3423|משנה, 3423|המקורי, 3420|לתת, 3411|דיון, 3401|הקמת, 3398|להקת, 3395|מסוימים, 3395|הצבעה, 3392|מסוימת, 3391|החוק, 3381|אברהם, 3378|עצמם, 3372|לנו, 3363|דין, 3357|עת, 3357|לצורך, 3356|השונים, 3356|ראה, 3356|בברכה, 3353|גילגמש, 3345|חשיבות, 3327|ורק, 3321|די, 3319|נושא, 3319|גדולים, 3318|סביב, 3317|קו, 3313|בתפקיד, 3310|השפה, 3309|לכאן, 3308|לדעת, 3308|העובדה, 3306|התיכון, 3301|למנוע, 3301|הסדרה, 3298|תושבי, 3296|מבנה, 3288|לקראת, 3283|בעלות, 3281|כדור, 3277|היחיד, 3266|אפריל, 3266|בריטניה, 3265|הישראלית, 3262|תואר, 3261|במרכז, 3249|הפועל, 3246|מוזיקה, 3236|תחילת, 3233|בדיוק, 3231|הר, 3224|הדרך, 3223|בשלב, 3220|ממנה, 3217|שטח, 3216|הכנסייה, 3200|דוגמה, 3200|המדינות, 3189|ובכך, 3179|ללמוד, 3178|הטבע, 3174|העצמאות, 3172|חזר, 3170|מאחר, 3168|לך, 3166|היחידה, 3165|ואני, 3161|אבן, 3157|המתאים, 3154|ק\"מ, 3154|הוקמה, 3152|למעלה, 3150|המשיך, 3135|סדר, 3129|נמצאת, 3127|לבצע, 3126|הקהילה, 3125|בגרמניה, 3125|שאינו, 3124|הוקם, 3121|דרום, 3119|העיקרי, 3114|הכוחות, 3113|לעיר, 3104|נפטר, 3104|ישראלי, 3098|הימים, 3093|אפריקה, 3084|בסופו, 3083|לבסוף, 3079|לקרוא, 3078|להעביר, 3076|גבוה, 3074|עשרה, 3073|עולה, 3073|למקום, 3070|עולם, 3068|בוויקיפדיה, 3065|במקביל, 3060|באתר, 3054|כה, 3054|מסוג, 3052|אור, 3048|קצת, 3041|במדינה, 3040|אתר, 3039|פברואר, 3038|בדומה, 3032|חומר, 3030|בפרס, 3026|הגוף, 3025|אלבום, 3024|משמעות, 3023|מטר, 3023|שאר, 3022|מערכות, 3012|האזור, 3009|ידע, 3001|חי, 2993|הטוב, 2988|רמת, 2986|האחרונה, 2985|הגרמני, 2981|מציע, 2980|הציבור, 2979|מלבד, 2963|מזה, 2962|מקובל, 2961|ארבע, 2957|מטרים, 2955|עריכה, 2955|המוזיקה, 2953|המשך, 2950|ראוי, 2949|תהיה, 2947|מספיק, 2946|לידי, 2946|הראשונות, 2943|מדוע, 2942|חיילים, 2941|מת, 2936|נכתב, 2933|בר, 2931|אמריקה, 2923|שאינם, 2920|רפאים, 2907|המבנה, 2901|ינואר, 2899|תבנית, 2895|נשיא, 2890|הקשר, 2889|טרול, 2883|רואה, 2881|במזרח, 2878|אוקטובר, 2877|ההגנה, 2877|הביניים, 2876|שיר, 2875|נהר, 2875|אליה, 2875|הרפובליקה, 2874|קטנה, 2872|למחוק, 2869|לישראל, 2867|ואז, 2865|וכל, 2864|יד, 2862|הדין, 2857|ברוב, 2856|נמצאים, 2853|ההיסטוריה, 2849|מצרים, 2849|תמונות, 2846|מרבית, 2846|וב-, 2843|למען, 2839|כמובן, 2834|ושל, 2833|מין, 2831|צפון, 2827|ים, 2825|שגם, 2825|בעיה, 2824|שבין, 2823|כולם, 2823|אותן, 2819|חברות, 2815|המכונה, 2815|היתר, 2810|בשני, 2808|פרסם, 2807|כחלק, 2797|ואם, 2797|ביקורת, 2784|שדה, 2783|נולדה, 2783|במטרה, 2781|הכוח, 2778|מקרה, 2774|הבחירות, 2774|כמות, 2773|שחקן, 2772|היטלר, 2769|אנגליה, 2768|המשפחה, 2767|מיוחד, 2766|גוף, 2766|לשעבר, 2763|ערב, 2760|העברי, 2751|מפלגת, 2749|הגדולים, 2747|תושבים, 2745|אנו, 2741|ארבעה, 2740|הלאומי, 2736|הרומית, 2736|בבחירות, 2733|רוסיה, 2733|המרכזי, 2730|המונח, 2729|להביא, 2728|ואין, 2727|מעמד, 2724|נקודות, 2716|שיחק, 2716|המצב, 2710|כאלה, 2709|הצטרף, 2707|מטעם, 2705|לבנון, 2702|זכות, 2700|אמר, 2696|קבוע, 2696|מעולם, 2692|כיצד, 2690|עשרות, 2688|עשר, 2688|החליט, 2684|ביחס, 2683|עבד, 2680|הצעיר, 2677|ביטוי, 2673|חזרה, 2672|מדיניות, 2671|לשנת, 2668|וזאת, 2667|חצי, 2664|סדרת, 2662|בא, 2656|בתחילה, 2655|מזרח, 2655|להצלחה, 2648|משפחת, 2644|הלאומית, 2642|אחיו, 2641|האל, 2640|השונות, 2637|לקיים, 2633|חוסר, 2633|חלקים, 2631|מבצע, 2630|בניו, 2613|עמד, 2613|קטנים, 2612|לעבור, 2611|מתחת, 2611|טובה, 2610|מכונה, 2607|ובשנת, 2605|עברה, 2599|השמש, 2599|מפקד, 2598|בתל, 2598|שלטון, 2593|אמנם, 2590|לחיצה, 2588|נוצר, 2588|חסר, 2587|פעולות, 2586|בראשית, 2586|לשמור, 2585|כזו, 2581|הסכם, 2580|נבחרת, 2579|אומר, 2578|השפעה, 2573|חדשות, 2570|חברה, 2568|עץ, 2561|הופיע, 2556|עניין, 2552|שיטת, 2550|הועבר, 2547|רקע, 2547|הידוע, 2545|בשפה, 2544|המערכת, 2542|חברים, 2539|אלף, 2539|יכולת, 2539|משרד, 2539|נהוג, 2538|איטליה, 2538|אלכסנדר, 2538|נשק, 2538|מושג, 2535|בעיות, 2535|תורת, 2534|קרובות, 2533|אלפי, 2532|נתן, 2531|עכשיו, 2530|הבאים, 2530|התרבות, 2529|בשימוש, 2528|לעבוד, 2527|יוצרים, 2522|אשמח, 2520|האוכלוסייה, 2516|הנוכחי, 2512|חיפה, 2510|תרבות, 2506|הקיסר, 2504|שעות, 2503|ישנה, 2503|שווה, 2503|ובו, 2501|דמות, 2500|כלשהו, 2499|האתר, 2496|לעולם, 2491|הצבאי, 2487|בכדי, 2486|בצרפת, 2484|לזה, 2482|החינוך, 2482|הגרמנים, 2481|באמת, 2477|האלה, 2477|ביחד, 2474|הצרפתית, 2462|ר', 2457|השתתף, 2456|הגעתם, 2454|סיפור, 2450|מאיר, 2447|מגיע, 2442|מקורות, 2439|ציון, 2438|קולנוע, 2436|אוניברסיטת, 2435|בפועל, 2433|בחלק, 2431|גורם, 2429|הבריטים, 2426|אפשרות, 2425|הקים, 2423|מלא, 2419|במערכת, 2414|השלישית, 2407|סרטים, 2407|אחוז, 2398|העשרים, 2397|במשחק, 2396|שמעון, 2391|ספרו, 2390|בסגנון, 2388|ייתכן, 2385|לשני, 2383|חודש, 2383|פארק, 2382|לבן, 2380|רומא, 2379|לתפקיד, 2379|חוץ, 2377|פולין, 2377|הצרפתי, 2376|צבאי, 2369|טען, 2365|פון, 2364|במדינות, 2363|התכוונת, 2360|במהירות, 2357|קרוב, 2357|הגרמנית, 2356|שיפנה, 2355|שבע, 2355|הארי, 2352|במצב, 2352|עומד, 2349|כולו, 2349|לשימוש, 2348|הגיעה, 2348|השירים, 2347|הרביעי, 2347|ספרד, 2346|אשתו, 2341|במקרים, 2341|הצליחו, 2339|סיום, 2338|לחלק, 2338|פעיל, 2336|שעל, 2336|נודה, 2333|בהיסטוריה, 2327|התורה, 2326|מאת, 2321|הביא, 2319|כותב, 2319|באי, 2315|באמצע, 2314|הייתי, 2313|ישנן, 2312|משני, 2309|דולר, 2303|כפר, 2303|תחזרו, 2303|באתם, 2303|ג'ורג', 2302|רשימת, 2302|משפחתו, 2299|ותתקנו, 2298|הדברים, 2297|לומר, 2296|יהודית, 2294|מחקר, 2291|היטב, 2290|רשת, 2288|א', 2282|הסיבה, 2280|התמונה, 2275|האמריקאי, 2274|שלאחר, 2270|מורכב, 2269|יחסי, 2266|הבריטית, 2266|פרט, 2266|כוללת, 2265|מזון, 2263|הבעיה, 2260|ניסה, 2254|שער, 2250|שינויים, 2249|המשתמש, 2246|ספרי, 2245|כנסת, 2244|הילדים, 2243|כהן, 2240|חוקי, 2239|שכתב, 2239|יפה, 2239|כסף, 2238|כשהוא, 2237|דקות, 2234|הצי, 2233|ועם, 2231|וזה, 2231|במקומות, 2230|שמואל, 2229|המקדש, 2227|החלק, 2225|הרעיון, 2224|הטלוויזיה, 2223|מרכזי, 2222|גן, 2222|דצמבר, 2221|להורג, 2220|במקור, 2217|זכו, 2217|להפוך, 2217|עשה, 2216|המערבית, 2212|התבנית, 2210|הפכו, 2205|ושם, 2203|המועצה, 2202|צבי, 2198|פרטים, 2196|הכדורגל, 2193|רחב, 2192|וכי, 2192|לימודיו, 2191|עלי, 2189|מילים, 2188|גישה, 2187|ועדת, 2174|קל, 2171|במערב, 2169|קיימות, 2164|יתר, 2160|להבין, 2158|תנועה, 2158|בעניין, 2157|יהיו, 2157|המהפכה, 2154|מבחינת, 2150|תורה, 2149|דם, 2147|סמל, 2145|ראשית, 2144|לשלטון, 2143|ד\"ר, 2140|דמויות, 2140|הכפר, 2137|בשביל, 2136|הזאת, 2131|מיד, 2128|מראש, 2128|פנים, 2127|גורמים, 2126|מבלי, 2125|ויליאם, 2124|יוצא, 2124|עברו, 2124|דן, 2124|יחידות, 2123|סוגי, 2123|שנתיים, 2123|ארוך, 2121|האחרים, 2119|טלוויזיה, 2115|הדף, 2113|עוסק, 2112|השטח, 2111|תחום, 2110|משפחה, 2110|מעין, 2109|דגל, 2102|סופר, 2101|לוי, 2101|קיבלה, 2100|תרגום, 2100|בעיני, 2098|התנגדות, 2095|קבע, 2091|בשטח, 2090|מדעי, 2087|להופיע, 2085|לבנות, 2084|האחרונים, 2082|לתרום, 2082|המערבי, 2080|במלחמה, 2078|לונדון, 2077|גדל, 2074|גדולות, 2073|שמונה, 2072|ספק, 2069|ידועה, 2069|מחוז, 2069|נפוץ, 2068|הבאה, 2068|כדורגל, 2064|האזרחים, 2063|הממלכה, 2058|סגנון, 2058|עשוי, 2057|כעת, 2053|ג'יימס, 2048|מנסה, 2045|לעברית, 2044|ה', 2044|דרכו, 2043|השחקן, 2043|ולעתים, 2042|המזרחי, 2041|צעיר, 2039|כוכב, 2038|מאמרים, 2037|הדת, 2036|הסיפור, 2034|נחשבת, 2033|בנוגע, 2032|הנוער, 2031|לצאת, 2027|בצורת, 2026|שיצא, 2022|הפעולה, 2022|אורי, 2021|סיבה, 2021|אדום, 2020|לכנסת, 2019|נכנס, 2017|יצאו, 2017|למספר, 2016|נקראת, 2014|שבהן, 2010|אינן, 2008|להמשיך, 2007|איי, 2007|הספרים, 2006|אורך, 2006|בחודש, 2004|האדום, 2003|השיטה, 2002|צורת, 2002|חג, 2000|הצפוני, 1999|שלושת, 1999|לתוך, 1995|לגרום, 1995|נתונים, 1994|בעצם, 1992|השואה, 1989|בחברה, 1989|סין, 1988|מחשב, 1985|לטובת, 1984|גרם, 1983|קול, 1982|הקריירה, 1982|בתוכנית, 1980|הוחלט, 1977|המרכז, 1976|ישו, 1974|אותי, 1974|מכבי, 1972|גרסה, 1972|האות, 1970|רעיון, 1970|רצח, 1969|פוליטית, 1969|שפה, 1966|האנשים, 1965|פתח, 1964|העונה, 1964|ספטמבר, 1962|רוק, 1962|למדי, 1962|לשמש, 1960|מיני, 1960|אמור, 1960|להקים, 1959|אודות, 1958|זוהי, 1957|בטוח, 1955|שיטה, 1955|המרכזית, 1950|ישיבת, 1950|נמוך, 1949|נודע, 1948|המחשב, 1947|מכיל, 1946|השאלה, 1945|סיים, 1942|הביטחון, 1941|בסדרה, 1941|מבקש, 1941|רוח, 1938|הכללי, 1935|פיתוח, 1935|עושה, 1933|הכולל, 1929|העת, 1927|האנגלי, 1927|ניכר, 1924|ספרות, 1922|שלנו, 1922|הביאה, 1920|מערב, 1917|הבאות, 1914|כיהן, 1913|לארצות, 1913|ב', 1911|בהרבה, 1910|בספרו, 1909|שירת, 1909|דת, 1909|השניים, 1908|הנרי, 1908|יצירות, 1902|רשימה, 1900|החלל, 1899|נקודת, 1898|רשמי, 1897|כדוגמת, 1897|החומר, 1897|הזהב, 1896|זהה, 1895|לעמוד, 1891|ארכיון, 1890|נבנה, 1889|כרגע, 1888|חבל, 1888|מהן, 1883|עצם, 1881|חמש, 1881|להשיג, 1880|מסע, 1880|הבינלאומי, 1879|יסוד, 1876|מידי, 1873|המרד, 1872|הקבוצות, 1871|קטנות, 1871|אופן, 1870|התפתחות, 1869|עסק, 1869|יצירת, 1866|שיהיה, 1866|לפעול, 1865|הביטוי, 1864|צריכה, 1863|הקולנוע, 1863|קטגוריה, 1861|תאריך, 1861|התעופה, 1859|המודרנית, 1857|חמישה, 1856|הגדולות, 1853|נוסדה, 1851|חייב, 1851|היוונית, 1851|ממלכת, 1849|אנגלית, 1848|אנחנו, 1847|לאדם, 1846|מופיעה, 1844|מנהל, 1844|לכיוון, 1843|הגבול, 1842|משתנה, 1842|בעונת, 1841|בלונדון, 1841|אליהם, 1841|אוויר, 1840|סוגים, 1839|מקומות, 1837|ע\"י, 1836|הערים, 1835|שיטות, 1835|עובר, 1834|שרון, 1832|באוניברסיטה, 1831|גיל, 1831|המספרים, 1828|חיי, 1827|נושאים, 1826|יצר, 1824|הערבי, 1823|מילה, 1823|תחילה, 1823|החוף, 1819|הנהר, 1819|ישנו, 1818|לשחק, 1818|לחץ, 1816|להלן, 1814|מחיקה, 1814|חינוך, 1811|נהרגו, 1809|הנאצים, 1809|העיקריים, 1808|הרכבת, 1808|המספר, 1808|מספרים, 1806|בצבא, 1805|הוועדה, 1804|משחקים, 1804|הצורך, 1804|החוץ, 1803|מעלה, 1803|הגבוה, 1803|עזב, 1803|לחזור, 1800|האיחוד, 1799|הגנה, 1797|עברית, 1796|מתאים, 1796|עובד, 1793|חלקי, 1793|סימן, 1792|המאמר, 1792|המקור, 1791|הכל, 1790|יוון, 1789|נקבע, 1787|גילה, 1785|היסטוריה, 1785|ביניהן, 1783|המשנה, 1782|לאפשר, 1781|בראשות, 1781|הנאצית, 1781|הביאו, 1780|מתאר, 1779|יוכל, 1779|הקרקע, 1778|למעט, 1778|שב, 1775|שנקרא, 1774|הישיבה, 1771|מאד, 1770|פונקציה, 1770|ואחרים, 1769|העיקרית, 1769|דוגמאות, 1768|בקשר, 1766|שבת, 1765|רואים, 1764|הנ\"ל, 1764|לתקן, 1764|אמנות, 1763|גבי, 1761|שפות, 1760|גוריון, 1759|נשאר, 1754|משמעותי, 1754|נובמבר, 1753|נאלץ, 1753|אש, 1751|הופעות, 1750|הפרלמנט, 1747|המושג, 1746|במחלוקת, 1745|משפחות, 1744|תפקידו, 1744|כבוד, 1743|התקשורת, 1741|לאתר, 1741|בדבר, 1740|גודל, 1738|ניסיון, 1737|כאילו, 1736|אמצעי, 1735|על-פי, 1734|יחיד, 1734|מוצא, 1734|הפעם, 1733|הבסיס, 1732|אגב, 1731|ובעיקר, 1729|בבריטניה, 1728|צבע, 1728|להציג, 1727|שחור, 1725|ליצירת, 1724|לספירה, 1721|הליגה, 1720|נחל, 1720|בירת, 1719|האמריקני, 1719|המחקר, 1719|במדינת, 1719|הבירה, 1718|ברזיל, 1718|חופשי, 1717|עיקר, 1715|מצוי, 1714|בעצמו, 1713|הרוסית, 1713|להסביר, 1713|היתה, 1711|התושבים, 1711|נאמר, 1710|הצדדים, 1709|בבתי, 1708|ובמיוחד, 1707|נערך, 1707|בצד, 1707|משמעותית, 1705|לציון, 1702|החמישי, 1701|מיוחדת, 1701|הוציא, 1698|מאמר, 1697|העיתון, 1697|רבין, 1696|שאלה, 1694|באנגליה, 1692|הודו, 1692|עזה, 1689|לעזור, 1689|כאמור, 1689|מטוסי, 1688|מקבל, 1682|איננו, 1678|שירה, 1675|לשים, 1674|יוצר, 1674|לאומי, 1674|הכי, 1674|הבכורה, 1673|הדם, 1672|ואפילו, 1672|מתחיל, 1671|צוות, 1671|פרי, 1671|איזה, 1671|למשך, 1670|משם, 1668|בסדר, 1668|הצפון, 1668|ארוכה, 1667|לוויקיפדיה, 1667|מראה, 1664|שמדובר, 1662|הלבן, 1661|פרטי, 1659|המלוכה, 1658|ערים, 1658|הדרומי, 1657|המשטרה, 1655|עדיף, 1655|הרשמי, 1653|הקודם, 1653|שירות, 1652|חומרים, 1650|שאפשר, 1649|באזורים, 1649|מינים, 1649|מגוון, 1645|אסיה, 1644|הקיבוץ, 1643|ישראלית, 1642|כאלו, 1641|בעד, 1640|בכמה, 1640|אבי, 1637|דעות, 1637|פרש, 1634|משחקי, 1634|ששת, 1634|עורך, 1633|נפרד, 1632|מועצת, 1631|הקהל, 1631|בגוף, 1630|הרכב, 1629|השחור, 1629|טיפול, 1628|עין, 1628|מורכבת, 1628|זהב, 1628|ספינות, 1627|לבדוק, 1626|אדוארד, 1626|העוסק, 1626|טוענים, 1626|בערוץ, 1625|אישית, 1624|רשמית, 1623|דמותו, 1622|כלשהי, 1621|יהדות, 1621|צעירים, 1620|אישי, 1620|עזרה, 1620|אחדים, 1619|אזרחים, 1617|ברק, 1617|והחל, 1616|לגמרי, 1614|מיוחדים, 1613|הרוח, 1613|טרור, 1612|כאחד, 1612|ביצוע, 1611|מטוס, 1611|הסדר, 1611|האדמה, 1610|מעשה, 1610|שבועות, 1608|עמדה, 1606|מכאן, 1605|דרכים, 1605|השלום, 1604|קצרה, 1603|העניין, 1603|קיבלו, 1600|בזה, 1599|תקשורת, 1599|בסדרת, 1598|תנאי, 1598|אחראי, 1598|קיום, 1598|קיסר, 1597|כולה, 1595|קוראים, 1594|הדגל, 1594|פעל, 1594|הדמויות, 1593|הכלכלי, 1593|בשעה, 1592|לאזור, 1592|עובדים, 1592|הקדוש, 1591|חד, 1590|היצירה, 1590|שלך, 1590|משקל, 1589|רשום, 1589|המטוס, 1588|לאחרונה, 1588|סמוך, 1588|בפרט, 1587|וכדומה, 1587|אחדות, 1587|מבוסס, 1586|מלאה, 1586|המפורסם, 1586|האמריקאית, 1586|המילים, 1584|יפן, 1584|פירוט, 1584|טבעי, 1583|המין, 1583|שש, 1583|האפיפיור, 1582|מידה, 1582|אנרגיה, 1582|ממוצא, 1580|בניית, 1577|השתמשו, 1577|למי, 1576|לקחת, 1576|אפר', 1576|ברורה, 1575|אוסף, 1575|לעיל, 1575|חלקם, 1575|עתה, 1575|החיילים, 1574|הלא, 1572|קרא, 1572|חשובה, 1572|שירי, 1571|העלייה, 1570|בתהליך, 1570|מתייחס, 1570|זוכה, 1569|למלחמה, 1569|רוברט, 1569|כעבור, 1569|לימוד, 1568|קרן, 1566|לשלב, 1565|רובם, 1564|לדבר, 1563|משמשת, 1562|דעת, 1561|הטקסט, 1560|חשובים, 1560|דברי, 1560|האינטרנט, 1560|גל, 1559|גברים, 1557|בשתי, 1557|הינם, 1557|דניאל, 1557|לעצמו, 1556|לראש, 1556|אירלנד, 1556|גידול, 1555|שניים, 1555|החולים, 1555|מוזמנים, 1555|לעבר, 1552|האוניברסיטה, 1550|נמוכה, 1549|הצליחה, 1549|הקטן, 1549|עבודתו, 1549|אוסטריה, 1548|במחוז, 1548|עונה, 1547|אריה, 1547|טענה, 1546|יכולות, 1546|להגן, 1545|קבלת, 1545|תכונות, 1545|ובהם, 1542|להעלות, 1541|בהיותו, 1540|הצלחה, 1540|היות, 1540|זרם, 1539|כיבוש, 1539|באה, 1536|מסוימות, 1535|המקורית, 1534|בפעם, 1534|נפש, 1534|הופיעה, 1534|המשטר, 1534|הכול, 1533|בטרם, 1533|חוזה, 1533|פירוש, 1533|צריכים, 1533|סוריה, 1533|החשובים, 1531|שאם, 1531|פעמיים, 1529|מנחם, 1527|לפיכך, 1526|אלון, 1526|אמריקאי, 1526|המדע, 1525|ספורט, 1524|אמו, 1524|המזרחית, 1523|מאחורי, 1522|תוכן, 1522|הכוונה, 1522|תיאטרון, 1522|מופיעים, 1522|המסורת, 1521|מקרים, 1521|התזמורת, 1519|מצליח, 1519|טוען, 1518|המשיכה, 1517|גדי, 1516|טעם, 1516|ממשלה, 1516|לקבוע, 1516|בגין, 1515|תוצאות, 1514|הקו, 1513|הפוליטית, 1511|הוביל, 1510|בפריז, 1510|המקומית, 1510|לפתוח, 1510|השמונים, 1509|ידועים, 1509|צבאית, 1508|מעלות, 1508|מדבר, 1508|באיטליה, 1507|התחיל, 1506|הצבאית, 1505|חזק, 1505|פרשת, 1504|שלם, 1504|המזרח, 1503|העולמי, 1503|שצריך, 1502|שינה, 1501|פבר', 1501|שלטונו, 1500|שכבר, 1498|שהן, 1498|להוציא, 1498|באליפות, 1497|בנימין, 1496|הנידון, 1496|תופעה, 1495|מנהיג, 1495|הנשק, 1495|המקובל, 1494|חוזר, 1494|הפנים, 1492|אדמה, 1491|סדרה, 1491|וחצי, 1489|הגישה, 1489|פרסום, 1489|תנועות, 1488|באחד, 1486|עובדה, 1486|לואיס, 1486|האמת, 1486|היהדות, 1486|בנושאים, 1485|המשחקים, 1484|אחריו, 1483|נמצאו, 1483|נפוליאון, 1482|נתון, 1482|כבד, 1482|דפי, 1482|הממשל, 1482|תוכניות, 1481|תום, 1479|אישה, 1479|במרחב, 1478|ההצבעה, 1477|רחוב, 1477|הסבר, 1477|במה, 1476|איננה, 1476|התואר, 1476|נובע, 1476|מוכר, 1475|מפתח, 1475|הפועלים, 1475|פול, 1472|מטאל, 1470|ערכי, 1470|הסביבה, 1469|בתואר, 1468|שאף, 1468|למוזיקה, 1467|הרשות, 1467|המנדט, 1467|מוות, 1466|באשר, 1464|יוסי, 1464|השחקנים, 1463|איחוד, 1463|הסרטים, 1461|בצה\"ל, 1460|שערים, 1460|נותר, 1460|הכיבוש, 1459|שאינה, 1458|הקשורים, 1457|באלבום, 1456|נקודה, 1455|ויותר, 1454|מכיר, 1454|רדיו, 1453|כלכלית, 1452|שליטה, 1451|האיים, 1451|אהבה, 1451|פוטר, 1450|מצפון, 1449|עצמי, 1448|למשפחה, 1448|מוחמד, 1448|להתמודד, 1445|ברלין, 1442|השבעים, 1440|ובה, 1439|השליטה, 1438|אנא, 1438|בנות, 1437|תת, 1437|שהסתיימו, 1435|בתו, 1435|לספר, 1432|פועל, 1430|מקווה, 1430|ברשת, 1429|שפת, 1428|מרדכי, 1428|תאי, 1427|סביר, 1425|עלתה, 1425|היסוד, 1424|בניין, 1424|מבוססת, 1423|להשאיר, 1419|מבית, 1419|שחקנים, 1418|סרטי, 1418|לילדים, 1418|גביע, 1417|אומרים, 1417|שניהם, 1416|בינתיים, 1416|הגבוהה, 1416|דומים, 1415|המוות, 1413|עושים, 1413|ימינו, 1413|נוער, 1412|האחים, 1412|רשות, 1411|שיתוף, 1410|למדינה, 1410|שהערך, 1410|הכריז, 1409|שטחים, 1409|אופי, 1408|במשפט, 1406|טעות, 1406|שבט, 1406|התנ\"ך, 1406|טבע, 1405|כוללים, 1405|להרחיב, 1405|הלכה, 1404|מסוגל, 1403|מאפשר, 1402|לפעמים, 1401|בסמוך, 1399|האלים, 1398|קשות, 1397|מרחב, 1396|השישי, 1396|כראש, 1396|בהקשר, 1396|בערבית, 1396|יהושע, 1396|רכבת, 1395|שכתוב, 1394|ניסו, 1394|הונגריה, 1394|תמיכה, 1392|הסופר, 1390|בדיוני, 1390|לואי, 1389|מבני, 1388|פוליטי, 1388|בנה, 1388|לימים, 1387|להכניס, 1386|יצאה, 1386|חזקה, 1385|האור, 1385|הכללית, 1384|אלמוג, 1384|דרור, 1384|חולים, 1384|נכונה, 1383|המלכה, 1383|להן, 1383|רכב, 1382|חיות, 1382|לוקה, 1382|ילד, 1381|האקדמיה, 1379|מהווים, 1377|ישראלים, 1377|השפות, 1377|מעניין, 1377|בתחומי, 1376|שנערך, 1376|הבולטים, 1374|הנה, 1374|הנשים, 1374|לצבא, 1373|חברתית, 1373|הקטנה, 1373|מצא, 1373|תיאור, 1372|מהירות, 1372|הלל, 1370|הערבים, 1370|ואחר, 1370|חז\"ל, 1370|צורה, 1368|בערכים, 1366|מדע, 1366|דו, 1366|השניה, 1365|להניח, 1364|ברמת, 1364|איתו, 1363|תחנת, 1362|בגביע, 1361|האופרה, 1361|יחידה, 1360|דתית, 1360|פתרון, 1359|הדמות, 1356|יחס, 1355|לימודי, 1354|קישורים, 1354|דור, 1353|";
        }
        parseLines(content);
    }

    private void parseLines(String str) {
        String[] lines = str.split("\\|");
        Stream<String> stream = Stream.of(lines);
        stream.forEach(l -> {
            String[] split = l.split(FILE_DELIMITER);
            Integer occurrences = Integer.valueOf(split[OCCURRENCES_COLUMN].trim());
            String word = split[WORD_COLUMN];
            int length = word.length();
            if (length > 4 && length < 7) {
                if (
                        occurrences < 1_000 ||
                                word.contains("'") ||
                                word.contains("-") ||
                                word.contains("\"") ||
                                word.startsWith("ש") ||
                                word.startsWith("ב") ||
                                word.startsWith("כ") ||
                                word.startsWith("ה") ||
                                word.startsWith("ל") ||
                                word.endsWith("ים")
                        ) {
                    // pass
                } else {
                    wordBankList.add(word);
                }
            }
        });
        stream.close();
        wordBankList = wordBankList.subList(0, 30);
        List<Word> wordList = wordBankList.stream().map(w -> new Word(w, null)).collect(Collectors.toList());
        try {
            initClues(wordList);
        } catch (InterruptedException e) {
            LOG.error("error init clues: " + e.getMessage());
        }
        int before = wordBankList.size();
        wordList.forEach(word -> {
            if (word.getClue() == null) {
                String curWord = word.getWord();
                System.out.println("did not found clue for word [" + curWord + "]");
                wordBankList.remove(curWord);
            }
        });
        LOG.info("before: " + before + "\tafter: " + wordBankList.size());
    }

    private void initClues(List<Word> wordList) throws InterruptedException {
        ExecutorService es = Executors.newFixedThreadPool(wordList.size());

        wordList.forEach(word -> es.execute(() -> {
            String _word = word.getWord();
            String clue = this.getClue(_word);
            if (clue != null) {
                cache.put(_word, clue);
            }
            word.setClue(clue);
        }));

        es.shutdown();
        es.awaitTermination(60, TimeUnit.SECONDS);
    }

    @Override
    public List<Word> getRandomWords(int level) {
        List<Word> wordList = new ArrayList<>();
        Set<Integer> numbersAlreadyPicked = new HashSet<>();
        int numberOfRequiredWords = 12;
        int wordBankSize = wordBankList.size();
        for (int i = 0; i < numberOfRequiredWords; i++) {
            int randomNumber;
            do {
                randomNumber = ThreadLocalRandom.current().nextInt(wordBankSize);
            } while (numbersAlreadyPicked.contains(randomNumber));
            numbersAlreadyPicked.add(randomNumber);
            String word = wordBankList.get(randomNumber);
            wordList.add(new Word(word, null));
        }
        return wordList;
    }


    @Override
    public void getClues(List<Word> wordList) {
        ExecutorService es = Executors.newFixedThreadPool(wordList.size());
        wordList.forEach(word -> {
            LOG.debug("going for getClue " + word.getWord() + "\t" + word.toString());
//            System.out.println(word);
            es.execute(() -> {
                try {
                    word.setClue(cache.get(word.getWord()));
                } catch (ExecutionException e) {
                    LOG.error("error getting from cache: " + e.getMessage());
                }
            });
        });
        try {
            es.shutdown();
            es.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * http://memorynotfound.com/spring-boot-ehcache-2-caching-example-configuration/
     * http://www.codingpedia.org/ama/spring-caching-with-ehcache/
     * can add condition for cache, for example:  @Cacheable(condition = "#instrument.equals('trombone')")
     * @param query word to search in google
     * @return first image from google images results in base64
     * using LRU Cache
     * configuration @ ehcache.xml
     */
    private String getClue(String query) {
        // TODO extract to config
        final String userAgent = "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.116 Safari/537.36";
        final String url = "https://www.google.co.il/search?site=imghp&tbm=isch&source=hp&gws_rd=cr&tbs=isz:m&q=";
        final String referrer = "https://www.google.co.il/";
        final String querySelect1 = "div.rg_meta";
        final String jsonUrlKey = "ou";

        String imageBase64 = null;
        try {
            Document doc = Jsoup.connect(url + query).userAgent(userAgent).referrer(referrer).get();
            Elements elements = doc.select(querySelect1);
            String firstElement = elements.first().childNodes().get(0).toString();
            String ou = JsonParserFactory.getJsonParser().parseMap(firstElement).get(jsonUrlKey).toString();
            byte[] imageBytes = IOUtils.toByteArray(new URL(ou));
            imageBase64 = Base64.getEncoder().encodeToString(imageBytes);
            LOG.debug("fetching clue \t" + query + "\n" + ou + "\n" + imageBase64.length());
        } catch (Exception e) {
            LOG.error(e.getMessage());
            // TODO handle exception.. either return text with the clue and "sorry, here is the solution" or try again
        }
        return imageBase64;
    }

    public void setCsvSourceFilePath(String csvSourceFilePath) {
        this.srcFilePath = csvSourceFilePath;
    }

}