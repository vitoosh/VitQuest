"""Generates app/src/main/assets/seed/foods.json.

Usage: python3 tools/seed/gen_foods.py app/src/main/assets/seed/foods.json
Values are per 100 g edible portion, rounded. After changing them, raise
SeedDataLoader.SEED_VERSION so existing installs pick up the update.
"""
import json, sys
# key, name_id, name_en, category, kcal, protein, carbs, fat per 100 g, source, servings [(id, en, grams)]
T="TKPI"; U="USDA"; E="ESTIMATE"
P=lambda g:[("1 porsi (%d g)"%g,"1 serving (%d g)"%g,g)]
F=[
# Staples
("nasi_putih","Nasi putih","Steamed white rice","STAPLE",180,3.0,39.8,0.3,T,[("1 centong (100 g)","1 scoop (100 g)",100),("1 piring (150 g)","1 plate (150 g)",150)]),
("nasi_merah","Nasi merah","Steamed brown rice","STAPLE",149,2.8,32.5,0.4,T,[("1 centong (100 g)","1 scoop (100 g)",100),("1 piring (150 g)","1 plate (150 g)",150)]),
("lontong","Lontong","Lontong (compressed rice cake)","STAPLE",144,2.5,32.0,0.2,E,[("1 potong (50 g)","1 piece (50 g)",50)]),
("kentang_rebus","Kentang rebus","Boiled potato","STAPLE",87,1.9,20.1,0.1,U,[("1 buah sedang (150 g)","1 medium potato (150 g)",150)]),
("ubi_rebus","Ubi jalar rebus","Boiled sweet potato","STAPLE",76,1.4,17.7,0.1,U,[("1 buah sedang (130 g)","1 medium (130 g)",130)]),
("jagung_rebus","Jagung rebus","Boiled sweet corn","STAPLE",96,3.4,21.0,1.5,U,[("1 tongkol (100 g pipilan)","1 cob (100 g kernels)",100)]),
("roti_tawar","Roti tawar putih","White bread","STAPLE",266,7.6,49.0,3.3,U,[("1 lembar (25 g)","1 slice (25 g)",25)]),
("roti_gandum","Roti gandum utuh","Whole-wheat bread","STAPLE",252,12.4,43.0,3.5,U,[("1 lembar (28 g)","1 slice (28 g)",28)]),
("oat_kering","Oat (kering)","Rolled oats (dry)","STAPLE",379,13.0,68.0,6.5,U,[("3 sdm (30 g)","3 tbsp (30 g)",30),("½ gelas (40 g)","½ cup (40 g)",40)]),
("mi_instan_goreng","Mi instan goreng (dimasak)","Instant fried noodles (prepared)","STAPLE",460,8.0,60.0,20.0,E,[("1 bungkus (85 g kering)","1 pack (85 g dry)",85)]),
# Protein
("telur_rebus","Telur rebus","Boiled egg","PROTEIN",155,12.6,1.1,10.6,U,[("1 butir (50 g)","1 egg (50 g)",50)]),
("telur_dadar","Telur dadar","Omelette (with oil)","PROTEIN",190,12.0,1.0,15.0,E,[("1 porsi (60 g)","1 serving (60 g)",60)]),
("telur_ceplok","Telur ceplok","Fried egg","PROTEIN",196,13.6,0.8,14.8,U,[("1 butir (46 g)","1 egg (46 g)",46)]),
("telur_mentah","Telur ayam (mentah)","Chicken egg (raw)","PROTEIN",143,12.6,0.7,9.5,U,[("1 butir (50 g)","1 egg (50 g)",50)]),
("dada_ayam_panggang","Dada ayam panggang tanpa kulit","Grilled skinless chicken breast","PROTEIN",165,31.0,0.0,3.6,U,[("1 potong (100 g)","1 piece (100 g)",100)]),
("dada_ayam_mentah","Dada ayam tanpa kulit (mentah)","Skinless chicken breast (raw)","PROTEIN",120,22.5,0.0,2.6,U,[("100 g","100 g",100)]),
("ayam_panggang","Ayam panggang/bakar (dengan kulit)","Grilled chicken with skin","PROTEIN",230,26.0,4.0,12.0,E,[("1 potong paha (90 g)","1 thigh piece (90 g)",90)]),
("ayam_goreng","Ayam goreng (dengan kulit)","Fried chicken with skin","PROTEIN",260,25.0,4.0,16.0,E,[("1 potong paha (90 g)","1 thigh piece (90 g)",90)]),
("ikan_nila_bakar","Ikan nila bakar","Grilled tilapia","PROTEIN",128,26.0,0.0,2.7,U,[("1 porsi (100 g)","1 serving (100 g)",100)]),
("ikan_nila_mentah","Ikan nila (mentah)","Tilapia (raw)","PROTEIN",96,20.1,0.0,1.7,U,[("100 g","100 g",100)]),
("ikan_goreng","Ikan kembung/tongkol goreng","Fried mackerel or tuna","PROTEIN",230,24.0,2.0,14.0,E,[("1 ekor kecil (80 g)","1 small fish (80 g)",80)]),
("pepes_ikan","Pepes ikan","Pepes (steamed spiced fish)","PROTEIN",150,20.0,3.0,6.0,E,[("1 bungkus (100 g)","1 parcel (100 g)",100)]),
("udang_rebus","Udang rebus","Boiled shrimp","PROTEIN",99,24.0,0.2,0.3,U,[("1 porsi (100 g)","1 serving (100 g)",100)]),
("rendang","Rendang daging","Beef rendang","PROTEIN",250,22.0,6.0,15.0,E,[("1 potong (50 g)","1 piece (50 g)",50)]),
("sate_ayam","Sate ayam dengan bumbu kacang","Chicken satay with peanut sauce","PROTEIN",225,20.0,8.0,12.0,E,[("5 tusuk (75 g)","5 skewers (75 g)",75)]),
("tempe","Tempe (mentah/kukus)","Tempeh (raw or steamed)","PROTEIN",201,20.8,13.5,8.8,T,[("1 potong (50 g)","1 piece (50 g)",50)]),
("tempe_goreng","Tempe goreng","Fried tempeh","PROTEIN",350,23.0,10.0,26.0,E,[("1 potong (30 g)","1 piece (30 g)",30)]),
("tempe_bacem","Tempe bacem","Sweet braised tempeh","PROTEIN",270,18.0,18.0,14.0,E,[("1 potong (40 g)","1 piece (40 g)",40)]),
("tahu","Tahu (mentah/kukus)","Tofu (raw or steamed)","PROTEIN",80,10.9,0.8,4.7,T,[("1 potong (50 g)","1 piece (50 g)",50)]),
("tahu_goreng","Tahu goreng","Fried tofu","PROTEIN",160,10.0,4.0,12.0,E,[("1 potong (50 g)","1 piece (50 g)",50)]),
("tahu_bacem","Tahu bacem","Sweet braised tofu","PROTEIN",150,9.0,12.0,8.0,E,[("1 potong (50 g)","1 piece (50 g)",50)]),
("bubur_kacang_hijau","Bubur kacang hijau","Mung bean porridge","DISH",130,4.0,20.0,4.0,E,[("1 mangkuk (200 g)","1 bowl (200 g)",200)]),
("susu_full_cream","Susu sapi full cream","Whole milk","DRINK",61,3.2,4.8,3.3,U,[("1 gelas (200 ml)","1 glass (200 ml)",200)]),
("susu_rendah_lemak","Susu rendah lemak","Low-fat milk","DRINK",42,3.4,5.0,1.0,U,[("1 gelas (200 ml)","1 glass (200 ml)",200)]),
("susu_kedelai","Susu kedelai tawar","Unsweetened soy milk","DRINK",41,3.3,3.0,1.8,E,[("1 gelas (200 ml)","1 glass (200 ml)",200)]),
("yogurt_plain","Yogurt plain","Plain yogurt","PROTEIN",61,3.5,4.7,3.3,U,[("1 cup (150 g)","1 cup (150 g)",150)]),
# Vegetables and vegetable dishes
("sayur_sop","Sayur sop","Indonesian vegetable soup","DISH",45,2.0,6.0,1.5,E,[("1 mangkuk (200 g)","1 bowl (200 g)",200)]),
("sayur_bayam_bening","Sayur bayam bening","Clear spinach soup","DISH",25,1.5,4.0,0.3,E,[("1 mangkuk (200 g)","1 bowl (200 g)",200)]),
("sayur_asem","Sayur asem","Sayur asem (tamarind vegetable soup)","DISH",35,1.2,7.0,0.5,E,[("1 mangkuk (200 g)","1 bowl (200 g)",200)]),
("sayur_lodeh","Sayur lodeh","Vegetables in coconut milk","DISH",85,2.5,6.0,6.0,E,[("1 mangkuk (200 g)","1 bowl (200 g)",200)]),
("tumis_kangkung","Tumis kangkung","Stir-fried water spinach","VEGETABLE",70,2.5,4.0,5.0,E,[("1 porsi (100 g)","1 serving (100 g)",100)]),
("capcay","Capcay","Stir-fried mixed vegetables","DISH",75,4.0,6.0,4.0,E,[("1 porsi (150 g)","1 serving (150 g)",150)]),
("gado_gado","Gado-gado (tanpa lontong)","Gado-gado (without rice cake)","DISH",135,6.0,10.0,8.0,E,[("1 porsi (250 g)","1 serving (250 g)",250)]),
("pecel","Pecel sayur","Pecel (vegetables with peanut sauce)","DISH",140,6.0,12.0,8.0,E,[("1 porsi (200 g)","1 serving (200 g)",200)]),
("urap","Urap sayur","Urap (vegetables with spiced coconut)","DISH",110,3.0,9.0,7.0,E,[("1 porsi (100 g)","1 serving (100 g)",100)]),
("lalapan","Lalapan (timun, kol, kemangi)","Fresh raw vegetables (lalapan)","VEGETABLE",15,0.8,3.0,0.1,E,[("1 porsi (50 g)","1 serving (50 g)",50)]),
("brokoli_rebus","Brokoli rebus","Boiled broccoli","VEGETABLE",35,2.4,7.2,0.4,U,[("1 mangkuk (90 g)","1 cup (90 g)",90)]),
("wortel_rebus","Wortel rebus","Boiled carrot","VEGETABLE",35,0.8,8.2,0.2,U,[("1 buah sedang (60 g)","1 medium carrot (60 g)",60)]),
("buncis_rebus","Buncis rebus","Boiled green beans","VEGETABLE",35,1.9,7.9,0.3,U,[("1 mangkuk (100 g)","1 cup (100 g)",100)]),
("labu_siam_rebus","Labu siam rebus","Boiled chayote","VEGETABLE",22,0.6,4.5,0.5,U,[("1 mangkuk (100 g)","1 cup (100 g)",100)]),
("kangkung","Kangkung (mentah)","Water spinach (raw)","VEGETABLE",19,2.6,3.1,0.2,U,[("1 ikat (250 g)","1 bunch (250 g)",250)]),
("bayam","Bayam (mentah)","Spinach (raw)","VEGETABLE",23,2.9,3.6,0.4,U,[("1 ikat (200 g)","1 bunch (200 g)",200)]),
("kol","Kol/kubis","Cabbage","VEGETABLE",25,1.3,5.8,0.1,U,[("1 mangkuk iris (70 g)","1 cup shredded (70 g)",70)]),
("mentimun","Mentimun","Cucumber","VEGETABLE",15,0.7,3.6,0.1,U,[("1 buah (100 g)","1 cucumber (100 g)",100)]),
("tomat","Tomat","Tomato","VEGETABLE",18,0.9,3.9,0.2,U,[("1 buah (100 g)","1 tomato (100 g)",100)]),
("brokoli","Brokoli (mentah)","Broccoli (raw)","VEGETABLE",34,2.8,6.6,0.4,U,[("1 mangkuk (90 g)","1 cup (90 g)",90)]),
("wortel","Wortel (mentah)","Carrot (raw)","VEGETABLE",41,0.9,9.6,0.2,U,[("1 buah sedang (60 g)","1 medium carrot (60 g)",60)]),
("buncis","Buncis (mentah)","Green beans (raw)","VEGETABLE",31,1.8,7.0,0.2,U,[("100 g","100 g",100)]),
# Seasonings and cooking ingredients
("bawang_merah","Bawang merah","Shallot","OTHER",72,2.5,16.8,0.1,U,[("1 siung (5 g)","1 clove (5 g)",5)]),
("bawang_putih","Bawang putih","Garlic","OTHER",149,6.4,33.0,0.5,U,[("1 siung (5 g)","1 clove (5 g)",5)]),
("cabai_merah","Cabai merah","Red chili","OTHER",40,1.9,8.8,0.4,U,[("1 buah (5 g)","1 chili (5 g)",5)]),
("jahe","Jahe","Ginger","OTHER",80,1.8,18.0,0.8,U,[("1 ruas (10 g)","1 knob (10 g)",10)]),
("jeruk_nipis","Air jeruk nipis","Lime juice","OTHER",25,0.4,8.4,0.1,U,[("1 sdm (15 g)","1 tbsp (15 g)",15)]),
("minyak_goreng","Minyak goreng","Cooking oil","OTHER",884,0.0,0.0,100.0,U,[("1 sdt (4 g)","1 tsp (4 g)",4),("1 sdm (13 g)","1 tbsp (13 g)",13)]),
("santan","Santan kental","Coconut milk (thick)","OTHER",230,2.3,5.5,23.8,U,[("100 ml","100 ml",100)]),
("kecap_manis","Kecap manis","Sweet soy sauce","OTHER",300,3.0,70.0,0.5,E,[("1 sdm (18 g)","1 tbsp (18 g)",18)]),
("gula_pasir","Gula pasir","Granulated sugar","OTHER",387,0.0,100.0,0.0,U,[("1 sdt (4 g)","1 tsp (4 g)",4),("1 sdm (12 g)","1 tbsp (12 g)",12)]),
("madu","Madu","Honey","OTHER",304,0.3,82.0,0.0,U,[("1 sdt (7 g)","1 tsp (7 g)",7)]),
("garam","Garam","Salt","OTHER",0,0.0,0.0,0.0,U,[("1 sdt (6 g)","1 tsp (6 g)",6)]),
("kacang_tanah","Kacang tanah sangrai","Dry-roasted peanuts","SNACK",585,24.0,21.0,50.0,U,[("1 genggam (30 g)","1 handful (30 g)",30)]),
("selai_kacang","Selai kacang","Peanut butter","OTHER",588,25.0,20.0,50.0,U,[("1 sdm (16 g)","1 tbsp (16 g)",16)]),
("sambal","Sambal","Chili sambal","OTHER",60,1.5,8.0,2.5,E,[("1 sdm (15 g)","1 tbsp (15 g)",15)]),
# Fruit
("pisang","Pisang","Banana","FRUIT",89,1.1,22.8,0.3,U,[("1 buah sedang (100 g)","1 medium banana (100 g)",100)]),
("pepaya","Pepaya","Papaya","FRUIT",43,0.5,10.8,0.3,U,[("1 potong (100 g)","1 slice (100 g)",100)]),
("semangka","Semangka","Watermelon","FRUIT",30,0.6,7.6,0.2,U,[("1 potong (150 g)","1 slice (150 g)",150)]),
("apel","Apel","Apple","FRUIT",52,0.3,13.8,0.2,U,[("1 buah (150 g)","1 apple (150 g)",150)]),
("jeruk","Jeruk","Orange","FRUIT",47,0.9,11.8,0.1,U,[("1 buah (130 g)","1 orange (130 g)",130)]),
("mangga","Mangga","Mango","FRUIT",60,0.8,15.0,0.4,U,[("½ buah (150 g)","½ mango (150 g)",150)]),
("nanas","Nanas","Pineapple","FRUIT",50,0.5,13.1,0.1,U,[("1 potong (100 g)","1 slice (100 g)",100)]),
("melon","Melon","Melon","FRUIT",34,0.8,8.2,0.2,U,[("1 potong (100 g)","1 slice (100 g)",100)]),
("jambu_biji","Jambu biji","Guava","FRUIT",68,2.6,14.3,1.0,U,[("1 buah (100 g)","1 guava (100 g)",100)]),
("salak","Salak","Snake fruit (salak)","FRUIT",77,0.4,20.9,0.0,E,[("2 buah (60 g)","2 fruits (60 g)",60)]),
("alpukat","Alpukat","Avocado","FRUIT",160,2.0,8.5,14.7,U,[("½ buah (70 g)","½ avocado (70 g)",70)]),
("buah_naga","Buah naga","Dragon fruit","FRUIT",57,1.2,13.0,0.4,E,[("½ buah (150 g)","½ fruit (150 g)",150)]),
# Mixed dishes
("nasi_goreng","Nasi goreng","Fried rice","DISH",170,5.0,25.0,6.0,E,[("1 piring (250 g)","1 plate (250 g)",250)]),
("mie_goreng","Mie goreng","Fried noodles","DISH",190,5.0,26.0,7.5,E,[("1 piring (200 g)","1 plate (200 g)",200)]),
("mie_ayam","Mie ayam","Chicken noodles","DISH",140,6.0,20.0,4.0,E,[("1 mangkuk (300 g)","1 bowl (300 g)",300)]),
("bakso","Bakso kuah (tanpa mie)","Meatball soup (no noodles)","DISH",70,6.0,5.0,3.0,E,[("1 mangkuk (300 g)","1 bowl (300 g)",300)]),
("soto_ayam","Soto ayam (tanpa nasi)","Chicken soto (without rice)","DISH",60,5.0,3.0,3.0,E,[("1 mangkuk (300 g)","1 bowl (300 g)",300)]),
("bubur_ayam","Bubur ayam","Chicken rice porridge","DISH",110,5.0,15.0,3.5,E,[("1 mangkuk (300 g)","1 bowl (300 g)",300)]),
# Snacks
("pisang_goreng","Pisang goreng","Fried banana","SNACK",250,2.0,35.0,12.0,E,[("1 buah (60 g)","1 piece (60 g)",60)]),
("bakwan","Bakwan sayur","Vegetable fritter","SNACK",280,5.0,30.0,16.0,E,[("1 buah (40 g)","1 piece (40 g)",40)]),
("kerupuk","Kerupuk","Crackers (kerupuk)","SNACK",500,5.0,65.0,25.0,E,[("1 genggam (10 g)","1 handful (10 g)",10)]),
# Drinks
("teh_manis","Teh manis","Sweet tea","DRINK",20,0.0,5.0,0.0,E,[("1 gelas (250 ml)","1 glass (250 ml)",250)]),
("teh_tawar","Teh tawar","Unsweetened tea","DRINK",1,0.0,0.3,0.0,U,[("1 gelas (250 ml)","1 glass (250 ml)",250)]),
("kopi_hitam","Kopi hitam tanpa gula","Black coffee (no sugar)","DRINK",2,0.1,0.0,0.0,U,[("1 cangkir (200 ml)","1 cup (200 ml)",200)]),
("kopi_susu","Es kopi susu (dengan gula)","Iced coffee with milk and sugar","DRINK",60,1.5,9.0,2.0,E,[("1 gelas (250 ml)","1 glass (250 ml)",250)]),
("jus_jeruk","Jus jeruk tanpa gula tambahan","Orange juice (no added sugar)","DRINK",45,0.7,10.4,0.2,U,[("1 gelas (200 ml)","1 glass (200 ml)",200)]),
]
keys=[f[0] for f in F]; assert len(keys)==len(set(keys))
out=[]
for k,nid,nen,cat,kc,p,c,f,src,serv in F:
    # sanity: Atwater energy should be close to stated kcal (±25%) for macro-bearing items
    atw=4*p+4*c+9*f
    if kc>20 and abs(atw-kc)/kc>=0.2: print("CHECK",k,cat,kc,round(atw))
    out.append({"key":k,"nameId":nid,"nameEn":nen,"category":cat,"kcal":kc,"protein":p,"carbs":c,"fat":f,"source":src,
                "servings":[{"labelId":a,"labelEn":b,"grams":g} for a,b,g in serv]})
json.dump({"version":1,"foods":out},open(sys.argv[1],'w'),ensure_ascii=False,indent=1)
print(len(out),"foods")
