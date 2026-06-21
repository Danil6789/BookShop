-- V4: prefix seed book cover URLs with /seed-covers/
-- V2 stored values as bare filenames (e.g. 'book-1.jpg'). Frontend needs full
-- path so the browser can resolve against the backend origin.
UPDATE books
SET cover_url = '/seed-covers/' || cover_url
WHERE cover_url IS NOT NULL
  AND cover_url <> ''
  AND cover_url NOT LIKE '/%';
