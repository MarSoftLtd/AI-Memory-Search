from pathlib import Path
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import A4

ROOT = Path(__file__).resolve().parents[2] / "output" / "pdf"
ROOT.mkdir(parents=True, exist_ok=True)
pdfmetrics.registerFont(TTFont("ValidationSans", r"C:\Windows\Fonts\arial.ttf"))

DOCUMENTS = {
    "mcanonical04_romanian.pdf": (
        "Document de validare romanesc",
        ["Factura pentru apa trebuie platita vineri.", "Contractul de inchiriere este semnat luni.",
         "Reteta de supa contine rosii, morcovi si busuioc.", "Nota medicala recomanda odihna si hidratare.",
         "Verset biblic: Domnul este pastorul meu.", "Sedinta proiectului incepe marti la ora zece."]),
    "mcanonical04_german.pdf": (
        "Deutsches Validierungsdokument",
        ["Die Wasserrechnung muss am Freitag bezahlt werden.", "Der Mietvertrag wird am Montag unterschrieben.",
         "Das Suppenrezept enthaelt Tomaten, Karotten und Basilikum.", "Die medizinische Notiz empfiehlt Ruhe und viel Wasser.",
         "Bibelvers: Der Herr ist mein Hirte.", "Die Projektbesprechung beginnt am Dienstag um zehn Uhr."]),
    "mcanonical04_french.pdf": (
        "Document francais de validation",
        ["La facture d'eau doit etre payee vendredi.", "Le contrat de location est signe lundi.",
         "La recette de soupe contient des tomates, des carottes et du basilic.", "La note medicale recommande du repos et de l'hydratation.",
         "Verset biblique : Le Seigneur est mon berger.", "La reunion du projet commence mardi a dix heures."]),
    "mcanonical04_greek.pdf": (
        "Ελληνικό έγγραφο επικύρωσης",
        ["Ο λογαριασμός νερού πρέπει να πληρωθεί την Παρασκευή.", "Το συμβόλαιο ενοικίασης υπογράφεται τη Δευτέρα.",
         "Η συνταγή σούπας περιέχει ντομάτες, καρότα και βασιλικό.", "Το ιατρικό σημείωμα συνιστά ξεκούραση και ενυδάτωση.",
         "Βιβλικό εδάφιο: Ο Κύριος είναι ο ποιμένας μου.", "Η συνάντηση του έργου αρχίζει την Τρίτη στις δέκα."]),
    "mcanonical04_turkish.pdf": (
        "Türkçe doğrulama belgesi",
        ["Su faturası cuma günü ödenmelidir.", "Kira sözleşmesi pazartesi günü imzalanır.",
         "Çorba tarifi domates, havuç ve fesleğen içerir.", "Tıbbi not dinlenme ve bol su önerir.",
         "Kutsal Kitap ayeti: Rab benim çobanımdır.", "Proje toplantısı salı günü saat onda başlar."]),
    "mcanonical04_russian.pdf": (
        "Русский документ проверки",
        ["Счет за воду нужно оплатить в пятницу.", "Договор аренды подписывается в понедельник.",
         "Рецепт супа содержит помидоры, морковь и базилик.", "Медицинская записка рекомендует отдых и питье воды.",
         "Библейский стих: Господь - пастырь мой.", "Совещание по проекту начинается во вторник в десять часов."]),
}

for filename, (title, lines) in DOCUMENTS.items():
    output = ROOT / filename
    page = canvas.Canvas(str(output), pagesize=A4)
    page.setTitle(title)
    page.setFont("ValidationSans", 18)
    page.drawString(56, 790, title)
    page.setFont("ValidationSans", 11)
    y = 744
    for number, line in enumerate(lines, 1):
        page.drawString(64, y, f"{number}. {line}")
        y -= 58
    page.setFont("ValidationSans", 9)
    page.drawString(56, 42, "M-CANONICAL-04 production multilingual validation corpus")
    page.save()
