document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll(".like-form").forEach((form) => {
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      const button = form.querySelector(".like-button");
      const count = form.querySelector(".like-value");
      if (!button || !count || button.disabled) return;

      button.disabled = true;
      try {
        const response = await fetch(form.dataset.likeUrl, {
          method: "POST",
          body: new FormData(form),
          headers: { Accept: "application/json" },
          credentials: "same-origin"
        });
        if (!response.ok || !response.headers.get("content-type")?.includes("application/json")) {
          throw new Error("Like request failed");
        }
        const result = await response.json();
        if (!result.success) {
          throw new Error(result.message || "Like request failed");
        }
        button.classList.toggle("liked", result.liked);
        button.setAttribute("aria-pressed", String(result.liked));
        count.textContent = String(result.likeCount);
      } catch (_error) {
        form.submit();
      } finally {
        button.disabled = false;
      }
    });
  });

  document.querySelectorAll(".delete-post-form").forEach((form) => {
    form.addEventListener("submit", (event) => {
      if (!window.confirm("このつぶやきを削除しますか？")) event.preventDefault();
    });
  });
});
