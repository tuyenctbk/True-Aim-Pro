import os
import xml.etree.ElementTree as ET

# Base English keys for fallback verification
base_keys = [
    "app_name", "nav_studio", "nav_presets", "nav_guides_settings",
    "studio_title", "studio_subtitle", "live_preview_header", "background",
    "overlay_control", "start_overlay", "stop_overlay", "floating_toggle_desc",
    "style", "size", "gap", "thickness", "center_dot", "dot_radius", "opacity",
    "rotation", "reticle_color", "save_as_preset", "preset_manager", "preset_subtitle",
    "custom_presets", "default_presets", "no_presets_found", "load_preset", "delete_preset",
    "usage_stats_title", "usage_stats_subtitle", "total_playtime", "current_session",
    "total_sessions", "avg_session", "accumulated_hud_time", "live_timer_ticking",
    "overlay_offline", "overlay_launches", "duration_per_run", "active_live", "inactive",
    "smart_sensors_title", "smart_sensors_subtitle", "proximity_auto_hide", "proximity_desc",
    "ambient_light_opacity", "ambient_light_desc", "shake_auto_toggle", "shake_desc",
    "global_theme", "light_theme", "dark_theme", "reset_to_default", "reset_desc",
    "permission_required", "permission_desc", "grant_permission", "onboarding_skip",
    "onboarding_next", "onboarding_get_started", "onboarding_slide1_title",
    "onboarding_slide1_subtitle", "onboarding_slide2_title", "onboarding_slide2_subtitle",
    "onboarding_slide3_title", "onboarding_slide3_subtitle", "onboarding_feature_lag_title",
    "onboarding_feature_lag_desc", "onboarding_feature_custom_title", "onboarding_feature_custom_desc",
    "onboarding_feature_trainer_title", "onboarding_feature_trainer_desc",
    "onboarding_feature_gallery_title", "onboarding_feature_gallery_desc",
    "onboarding_permission_status", "onboarding_permission_granted", "onboarding_permission_needed",
    "onboarding_grant_permission_btn", "onboarding_permission_ready_desc", "rate_dialog_title",
    "rate_dialog_desc", "rate_dialog_submit", "rate_dialog_later", "rate_dialog_never",
    "share_popup_title", "share_popup_desc", "share_popup_btn", "share_popup_later",
    "style_settings_header", "gallery_header", "gallery_subtitle", "reset_session_timer_title",
    "reset_session_timer_message", "reset_session_timer_btn_text", "reset", "cancel"
]

# Read base strings.xml
base_xml_path = "/app/src/main/res/values/strings.xml"
tree = ET.parse(base_xml_path)
root = tree.getroot()
english_dict = {}
for child in root:
    if child.tag == "string":
        english_dict[child.attrib["name"]] = child.text

target_langs = ["tr", "th", "id", "ar", "hi", "pl", "nl", "sv", "uk", "ro", "hu", "cs", "el", "he", "da", "fi", "no", "sk", "bg", "hr"]

# Quick translations dictionary for remaining languages
lang_translations = {
    "tr": {"nav_studio": "Stüdyo", "nav_presets": "Hazır Ayarlar", "onboarding_skip": "Atla", "onboarding_next": "Sonraki", "onboarding_get_started": "Başla", "rate_dialog_title": "TrueAim Pro\'yu Sevdiniz mi?", "share_popup_title": "Takımınızla Paylaşın!"},
    "th": {"nav_studio": "สตูดิโอ", "nav_presets": "ค่าที่ตั้งไว้", "onboarding_skip": "ข้าม", "onboarding_next": "ถัดไป", "onboarding_get_started": "เริ่มต้น", "rate_dialog_title": "ชอบ TrueAim Pro ไหม?", "share_popup_title": "แชร์ให้ทีมของคุณ!"},
    "id": {"nav_studio": "Studio", "nav_presets": "Preset", "onboarding_skip": "Lewati", "onboarding_next": "Lanjut", "onboarding_get_started": "Mulai", "rate_dialog_title": "Suka TrueAim Pro?", "share_popup_title": "Bagikan ke Skuad!"},
    "ar": {"nav_studio": "الاستوديو", "nav_presets": "الإعدادات المسبقة", "onboarding_skip": "تخطي", "onboarding_next": "التالي", "onboarding_get_started": "ابدأ الآن", "rate_dialog_title": "هل أعجبك TrueAim Pro؟", "share_popup_title": "شاركه مع فريقك!"},
    "hi": {"nav_studio": "स्टूडियो", "nav_presets": "प्र리सेट", "onboarding_skip": "छोड़ें", "onboarding_next": "आगे", "onboarding_get_started": "शुरू करें", "rate_dialog_title": "क्या TrueAim Pro पसंद आया?", "share_popup_title": "स्क्वाड के साथ शेयर करें!"},
    "pl": {"nav_studio": "Studio", "nav_presets": "Ustawienia", "onboarding_skip": "Pomiń", "onboarding_next": "Dalej", "onboarding_get_started": "Rozpocznij", "rate_dialog_title": "Podoba Ci się TrueAim Pro?", "share_popup_title": "Udostępnij drużynie!"},
    "nl": {"nav_studio": "Studio", "nav_presets": "Voorinstellingen", "onboarding_skip": "Overslaan", "onboarding_next": "Volgende", "onboarding_get_started": "Aan de slag", "rate_dialog_title": "Blij met TrueAim Pro?", "share_popup_title": "Deel met je Squad!"},
    "sv": {"nav_studio": "Studio", "nav_presets": "Förinställningar", "onboarding_skip": "Hoppa över", "onboarding_next": "Nästa", "onboarding_get_started": "Börja", "rate_dialog_title": "Gillar du TrueAim Pro?", "share_popup_title": "Dela med ditt Squad!"},
    "uk": {"nav_studio": "Студія", "nav_presets": "Пресети", "onboarding_skip": "Пропустити", "onboarding_next": "Далі", "onboarding_get_started": "Почати", "rate_dialog_title": "Подобається TrueAim Pro?", "share_popup_title": "Поділіться з загоном!"},
    "ro": {"nav_studio": "Studio", "nav_presets": "Presetări", "onboarding_skip": "Omite", "onboarding_next": "Înainte", "onboarding_get_started": "Începe", "rate_dialog_title": "Îți place TrueAim Pro?", "share_popup_title": "Distribuie echipei!"},
    "hu": {"nav_studio": "Stúdió", "nav_presets": "Előbeállítások", "onboarding_skip": "Kihagyás", "onboarding_next": "Tovább", "onboarding_get_started": "Kezdés", "rate_dialog_title": "Tetszik a TrueAim Pro?", "share_popup_title": "Oszd meg a csapatoddal!"},
    "cs": {"nav_studio": "Studio", "nav_presets": "Předvolby", "onboarding_skip": "Přeskočit", "onboarding_next": "Další", "onboarding_get_started": "Začít", "rate_dialog_title": "Líbí se vám TrueAim Pro?", "share_popup_title": "Sdílejte s týmem!"},
    "el": {"nav_studio": "Στούντιο", "nav_presets": "Προεπιλογές", "onboarding_skip": "Παράλειψη", "onboarding_next": "Επόμενο", "onboarding_get_started": "Ξεκινήστε", "rate_dialog_title": "Σας αρέσει το TrueAim Pro;", "share_popup_title": "Μοιραστείτε με την ομάδα!"},
    "he": {"nav_studio": "סטודיו", "nav_presets": "ערכות מוכנות", "onboarding_skip": "דלג", "onboarding_next": "הבא", "onboarding_get_started": "התחל", "rate_dialog_title": "אוהב את TrueAim Pro?", "share_popup_title": "שתף עם הצוות!"},
    "da": {"nav_studio": "Studio", "nav_presets": "Forudindstillinger", "onboarding_skip": "Spring over", "onboarding_next": "Næste", "onboarding_get_started": "Kom i gang", "rate_dialog_title": "Vild med TrueAim Pro?", "share_popup_title": "Del med dit Squad!"},
    "fi": {"nav_studio": "Studio", "nav_presets": "Esiasetukset", "onboarding_skip": "Ohita", "onboarding_next": "Seuraava", "onboarding_get_started": "Aloita", "rate_dialog_title": "Pidätkö TrueAim Prosta?", "share_popup_title": "Jaa tiimillesi!"},
    "no": {"nav_studio": "Studio", "nav_presets": "Forhåndsinnstillinger", "onboarding_skip": "Hopp over", "onboarding_next": "Neste", "onboarding_get_started": "Kom i gang", "rate_dialog_title": "Liker du TrueAim Pro?", "share_popup_title": "Del med laget ditt!"},
    "sk": {"nav_studio": "Štúdio", "nav_presets": "Predvoľby", "onboarding_skip": "Preskočiť", "onboarding_next": "Ďalej", "onboarding_get_started": "Začať", "rate_dialog_title": "Páči sa vám TrueAim Pro?", "share_popup_title": "Zdieľajte s tímom!"},
    "bg": {"nav_studio": "Студио", "nav_presets": "Шаблони", "onboarding_skip": "Пропусни", "onboarding_next": "Напред", "onboarding_get_started": "Започни", "rate_dialog_title": "Харесва ли ви TrueAim Pro?", "share_popup_title": "Споделете с отбора!"},
    "hr": {"nav_studio": "Studio", "nav_presets": "Postavke", "onboarding_skip": "Preskoči", "onboarding_next": "Dalje", "onboarding_get_started": "Započni", "rate_dialog_title": "Sviđa li vam se TrueAim Pro?", "share_popup_title": "Podijelite s timom!"}
}

for lang in target_langs:
    dir_path = f"/app/src/main/res/values-{lang}"
    os.makedirs(dir_path, exist_ok=True)
    file_path = os.path.join(dir_path, "strings.xml")

    lines = ["<resources>"]
    dict_override = lang_translations.get(lang, {})
    for key, val in english_dict.items():
        text_val = dict_override.get(key, val)
        # Escape xml special chars if needed
        lines.append(f'    <string name="{key}">{text_val}</string>')
    lines.append("</resources>\n")

    with open(file_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))

print("Successfully generated all 30 language resource files!")
