========================================
פתרון לשגיאת InvalidPathException
(נתיב עם תווים בעברית - "שולחן העבודה")
========================================

העבר את הפרויקט לתיקייה עם נתיב באנגלית בלבד:

שלב 1: פתח PowerShell או CMD מתוך תיקייה באנגלית
        (למשל: Win+R, הקלד cmd, Enter)

שלב 2: הרץ את הפקודות הבאות (התאם את השם אם צריך):

   mkdir C:\Users\mylov\projects\stores
   xcopy "C:\Users\mylov\OneDrive\שולחן העבודה\stores\multi-stores" "C:\Users\mylov\projects\stores\multi-stores" /E /I /H

   או עם robocopy (שומר יותר):

   mkdir C:\Users\mylov\projects\stores
   robocopy "C:\Users\mylov\OneDrive\שולחן העבודה\stores\multi-stores" "C:\Users\mylov\projects\stores\multi-stores" /E /COPY:DAT

שלב 3: סגור את IntelliJ. פתח את הפרויקט מהנתיב החדש:
        File -> Open -> C:\Users\mylov\projects\stores\multi-stores

שלב 4: Build -> Rebuild Project

אחרי שהכל עובד, אפשר למחוק את התיקייה הישנה תחת "שולחן העבודה" אם אתה לא צריך אותה.

========================================
