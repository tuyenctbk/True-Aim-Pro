import os
import glob
import re

res_dir = "app/src/main/res"
xml_files = glob.glob(os.path.join(res_dir, "values*", "strings.xml"))

for path in xml_files:
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()

    # 1. Fix tutorial_point4_desc -> add formatted="false" if not present
    if 'name="tutorial_point4_desc"' in content and 'formatted="false"' not in content:
        content = re.sub(
            r'<string name="tutorial_point4_desc">(.*?)</string>',
            r'<string name="tutorial_point4_desc" formatted="false">\1</string>',
            content
        )

    # 2. Fix unescaped single quotes inside <string ...>...</string>
    def fix_string_element(match):
        attrs = match.group(1)
        text = match.group(2)
        
        # Replace unescaped single quotes with \'
        # Do not re-escape \' or &apos;
        # A simple way: replace &apos; temporarily, unescape \', then replace all ' with \'
        temp_text = text.replace("&apos;", "___APOS___").replace("\\'", "'")
        fixed_text = temp_text.replace("'", "\\'").replace("___APOS___", "&apos;")
        
        return f'<string{attrs}>{fixed_text}</string>'

    content = re.sub(
        r'<string([^>]*)>([\s\S]*?)</string>',
        fix_string_element,
        content
    )

    with open(path, "w", encoding="utf-8") as f:
        f.write(content)

print(f"Successfully processed {len(xml_files)} strings.xml files.")
