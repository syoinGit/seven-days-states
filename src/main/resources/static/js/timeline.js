document.addEventListener("DOMContentLoaded", () => {
  const timelineRefreshNoticeMs = 5 * 60 * 1000;
  const refreshButton = document.querySelector("[data-refresh-page]");
  if (refreshButton) {
    refreshButton.addEventListener("click", () => window.location.reload());
    window.setTimeout(() => {
      refreshButton.classList.add("has-update");
      refreshButton.innerHTML = '<span aria-hidden="true">↑</span> 新しい観測があります';
    }, timelineRefreshNoticeMs);
  }

  const timelineItems = Array.from(document.querySelectorAll("[data-timeline-item]"));
  const timelineLoader = document.querySelector("[data-timeline-loader]");
  const timelineProgress = document.querySelector("[data-timeline-progress]");
  const initialTimelineItems = 18;
  const timelinePageSize = 15;
  let visibleTimelineItems = Math.min(initialTimelineItems, timelineItems.length);

  const renderTimelinePage = () => {
    visibleTimelineItems = Math.min(visibleTimelineItems, timelineItems.length);
    timelineItems.forEach((item, index) => { item.hidden = index >= visibleTimelineItems; });
    if (timelineProgress) timelineProgress.textContent = visibleTimelineItems < timelineItems.length
      ? `${timelineItems.length - visibleTimelineItems}件の過去ログ`
      : "観測済み";
    if (timelineLoader) timelineLoader.hidden = visibleTimelineItems >= timelineItems.length;
  };

  if (timelineLoader && timelineItems.length > initialTimelineItems) {
    renderTimelinePage();
    const loadMore = () => {
      visibleTimelineItems += timelinePageSize;
      renderTimelinePage();
    };
    timelineLoader.addEventListener("click", loadMore);
    if ("IntersectionObserver" in window) {
      const observer = new IntersectionObserver((entries) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          loadMore();
          if (visibleTimelineItems >= timelineItems.length) observer.disconnect();
        }
      }, { rootMargin: "240px 0px" });
      observer.observe(timelineLoader);
    }
  }

  const copyButton = document.querySelector("[data-copy-generation]");
  if (copyButton) {
    copyButton.addEventListener("click", async () => {
      await navigator.clipboard.writeText(document.querySelector("#generation-data").value);
      copyButton.textContent = "コピー済み";
    });
  }

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
