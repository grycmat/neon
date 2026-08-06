# Changelog — v1.2.0 → next release

Changes since the last version bump (`18b20cf`, v1.2.0).

## English

- Fixed login failing on instances that delegate sign-in to an external SSO/OIDC provider (e.g. Vivaldi Social), and on servers that could silently break login by sending chunked responses.
- Added support for follow requests: accept or reject incoming requests to follow your locked account right from Notifications, or from a dedicated Follow requests screen.
- Long posts in feeds and lists are now truncated with a "Show more" hint instead of taking over the whole screen — the full text still shows when you open the thread.
- Fixed the Post button getting clipped on narrower phone screens.
- Tapping the top bar on the Home tab now scrolls straight to the top and dismisses the "new toots" banner, same as tapping the banner itself.

## Polski

- Naprawiono logowanie na instancjach korzystających z zewnętrznego dostawcy logowania SSO/OIDC (np. Vivaldi Social) oraz na serwerach, które mogły po cichu zrywać logowanie, wysyłając odpowiedzi w trybie chunked.
- Dodano obsługę próśb o obserwowanie: możesz teraz zaakceptować lub odrzucić przychodzącą prośbę o obserwowanie Twojego zablokowanego konta bezpośrednio w Powiadomieniach albo na dedykowanym ekranie próśb o obserwowanie.
- Długie wpisy na osi czasu i listach są teraz skracane z podpowiedzią „Pokaż więcej" zamiast zajmować cały ekran — pełna treść nadal wyświetla się po otwarciu wątku.
- Naprawiono przycisk „Publikuj", który był przycinany na węższych ekranach telefonów.
- Kliknięcie górnego paska na karcie Główna przewija teraz od razu na górę i ukrywa baner „nowe wpisy", tak samo jak kliknięcie samego banera.
