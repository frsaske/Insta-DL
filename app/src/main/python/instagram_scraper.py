"""
Instagram Scraper Engine for InstaDL
Extracts Instagram Reels, Posts, Carousels, and IGTV without login.
Uses yt-dlp with resilient fallback to direct Instagram JSON / regex extraction.
Zero external dependencies beyond yt-dlp (uses Python standard library urllib).
"""
import re
import json
import urllib.parse
import urllib.request
import ssl

try:
    import yt_dlp
except ImportError:
    yt_dlp = None

# Create permissive SSL context for urllib
_SSL_CTX = ssl.create_default_context()
_SSL_CTX.check_hostname = False
_SSL_CTX.verify_mode = ssl.CERT_NONE


def sanitize_instagram_url(url: str) -> str:
    """Sanitize and normalize Instagram URL."""
    url = url.strip()
    parsed = urllib.parse.urlparse(url)
    clean_path = parsed.path
    if not clean_path.endswith('/'):
        clean_path += '/'
    return f"https://www.instagram.com{clean_path}"


def extract_shortcode(url: str) -> str:
    match = re.search(r'/(?:p|reel|reels|tv)/([^/?#&]+)', url)
    return match.group(1) if match else "media"


def fetch_via_ytdlp(url: str) -> dict:
    if not yt_dlp:
        raise Exception("yt-dlp is not available")

    ydl_opts = {
        'quiet': True,
        'no_warnings': True,
        'skip_download': True,
        'extract_flat': False,
        'socket_timeout': 15,
        'http_headers': {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36',
            'Accept-Language': 'en-US,en;q=0.9',
        }
    }

    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(url, download=False)
        if not info:
            raise Exception("yt-dlp failed to extract info")

        media_id = info.get('id') or extract_shortcode(url)
        author = info.get('uploader') or info.get('channel') or info.get('uploader_id') or "instagram_creator"
        caption = info.get('description') or info.get('title') or ""
        thumbnail = info.get('thumbnail') or ""
        
        media_items = []
        entries = info.get('entries')
        
        if entries and len(entries) > 1:
            media_type = "carousel"
            for idx, entry in enumerate(entries):
                e_url = entry.get('url') or ""
                e_thumb = entry.get('thumbnail') or thumbnail
                is_vid = entry.get('ext') == 'mp4' or entry.get('vcodec') not in (None, 'none') or bool(entry.get('video_ext'))
                media_items.append({
                    "id": f"{media_id}_{idx+1}",
                    "type": "video" if is_vid else "image",
                    "url": e_url,
                    "thumbnail": e_thumb
                })
        else:
            direct_url = info.get('url') or ""
            is_vid = info.get('ext') == 'mp4' or info.get('vcodec') not in (None, 'none')
            media_type = "video" if is_vid else "image"
            media_items.append({
                "id": media_id,
                "type": media_type,
                "url": direct_url or thumbnail,
                "thumbnail": thumbnail
            })

        return {
            "id": media_id,
            "title": caption[:120] if caption else f"Post by @{author}",
            "caption": caption,
            "author": author,
            "media_type": media_type,
            "thumbnail_url": thumbnail or (media_items[0]["thumbnail"] if media_items else ""),
            "media_items": media_items
        }


def fetch_via_direct_json(url: str) -> dict:
    shortcode = extract_shortcode(url)
    clean_url = sanitize_instagram_url(url)
    
    headers = {
        'User-Agent': 'Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
        'Accept-Language': 'en-US,en;q=0.9',
    }

    author = "instagram_creator"
    title = ""
    thumbnail = ""
    try:
        oembed_url = f"https://www.instagram.com/oembed/?url={urllib.parse.quote(clean_url)}"
        req = urllib.request.Request(oembed_url, headers=headers)
        with urllib.request.urlopen(req, timeout=8, context=_SSL_CTX) as resp:
            if resp.status == 200:
                data = json.loads(resp.read().decode('utf-8'))
                author = data.get("author_name") or author
                title = data.get("title") or ""
                thumbnail = data.get("thumbnail_url") or ""
    except Exception:
        pass

    media_items = []
    media_type = "video" if "/reel" in url or "/tv" in url else "image"
    
    try:
        req = urllib.request.Request(clean_url, headers=headers)
        with urllib.request.urlopen(req, timeout=10, context=_SSL_CTX) as resp:
            if resp.status == 200:
                html = resp.read().decode('utf-8', errors='ignore')
                
                og_vid = re.search(r'<meta property="og:video" content="([^"]+)"', html)
                if not og_vid:
                    og_vid = re.search(r'<meta property="og:video:secure_url" content="([^"]+)"', html)
                
                og_img = re.search(r'<meta property="og:image" content="([^"]+)"', html)
                
                og_title = re.search(r'<meta property="og:title" content="([^"]+)"', html)
                if og_title and not title:
                    title = og_title.group(1).replace("&amp;", "&").replace("&quot;", '"')
                
                author_match = re.search(r'@([a-zA-Z0-9_\.]+)', title)
                if author_match and author == "instagram_creator":
                    author = author_match.group(1)

                if og_vid:
                    video_url = og_vid.group(1).replace("&amp;", "&")
                    thumb_url = og_img.group(1).replace("&amp;", "&") if og_img else thumbnail
                    media_type = "video"
                    thumbnail = thumb_url or thumbnail
                    media_items.append({
                        "id": shortcode,
                        "type": "video",
                        "url": video_url,
                        "thumbnail": thumb_url
                    })
                elif og_img:
                    img_url = og_img.group(1).replace("&amp;", "&")
                    thumbnail = img_url
                    media_type = "image"
                    media_items.append({
                        "id": shortcode,
                        "type": "image",
                        "url": img_url,
                        "thumbnail": img_url
                    })
    except Exception:
        pass

    if not media_items:
        media_items.append({
            "id": shortcode,
            "type": media_type,
            "url": thumbnail or f"https://instagram.com/p/{shortcode}/media/?size=l",
            "thumbnail": thumbnail
        })

    return {
        "id": shortcode,
        "title": title or f"Post by @{author}",
        "caption": title,
        "author": author,
        "media_type": media_type,
        "thumbnail_url": thumbnail,
        "media_items": media_items
    }


def fetch_instagram_media(url: str) -> str:
    clean_url = sanitize_instagram_url(url)
    
    try:
        data = fetch_via_ytdlp(clean_url)
        if data and data.get("media_items") and len(data["media_items"]) > 0:
            return json.dumps(data)
    except Exception:
        pass

    try:
        data = fetch_via_direct_json(clean_url)
        return json.dumps(data)
    except Exception:
        shortcode = extract_shortcode(clean_url)
        return json.dumps({
            "id": shortcode,
            "title": f"Instagram Media ({shortcode})",
            "caption": "",
            "author": "instagram_user",
            "media_type": "video" if "/reel" in url else "image",
            "thumbnail_url": "",
            "media_items": [{
                "id": shortcode,
                "type": "video" if "/reel" in url else "image",
                "url": clean_url,
                "thumbnail": ""
            }]
        })
