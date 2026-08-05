package com.yuki.sevendays_states.service;

import com.yuki.sevendays_states.entity.M_Player;
import com.yuki.sevendays_states.entity.M_WebAccount;
import com.yuki.sevendays_states.entity.T_PlayerPost;
import com.yuki.sevendays_states.entity.T_PlayerPostLike;
import com.yuki.sevendays_states.repository.M_PlayerRepository;
import com.yuki.sevendays_states.repository.T_PlayerPostLikeRepository;
import com.yuki.sevendays_states.repository.T_PlayerPostRepository;
import com.yuki.sevendays_states.util.DisplayTimeFormatter;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerSocialService {

  private final T_PlayerPostRepository postRepository;
  private final T_PlayerPostLikeRepository likeRepository;
  private final M_PlayerRepository playerRepository;
  private final CurrentWebAccountService currentAccountService;
  private final DisplayTimeFormatter displayTimeFormatter = new DisplayTimeFormatter();

  @Transactional(readOnly = true)
  public List<PostView> feed(Authentication authentication) {
    Optional<M_WebAccount> current = currentAccountService.current(authentication);
    return postRepository.findTop50ByOrderByCreatedAtDescIdDesc().stream()
        .map(post -> toView(post, current.orElse(null)))
        .toList();
  }

  @Transactional
  public ActionResult createPost(Authentication authentication, String rawBody) {
    Optional<M_WebAccount> account = currentAccountService.current(authentication);
    if (account.isEmpty()) {
      return ActionResult.failure("投稿するにはログインしてください。");
    }
    if (account.get().getPlayerId() == null) {
      return ActionResult.failure("ゲームプレイヤーに紐付いたアカウントで投稿してください。");
    }
    String body = rawBody == null ? "" : rawBody.strip();
    if (body.isBlank()) {
      return ActionResult.failure("投稿内容を入力してください。");
    }
    if (body.length() > 1000) {
      return ActionResult.failure("投稿は1000文字以内で入力してください。");
    }
    if (playerRepository.findById(account.get().getPlayerId()).isEmpty()) {
      return ActionResult.failure("紐付いたゲームプレイヤーが見つかりません。");
    }
    T_PlayerPost post = new T_PlayerPost();
    post.setAccountId(account.get().getId());
    post.setPlayerId(account.get().getPlayerId());
    post.setBody(body);
    postRepository.save(post);
    return ActionResult.success("投稿しました。");
  }

  @Transactional
  public LikeResult toggleLike(Authentication authentication, Long postId) {
    Optional<M_WebAccount> account = currentAccountService.current(authentication);
    if (account.isEmpty()) {
      return LikeResult.failure("いいねするにはログインしてください。");
    }
    if (postId == null || postRepository.findById(postId).isEmpty()) {
      return LikeResult.failure("投稿が見つかりません。");
    }
    Optional<T_PlayerPostLike> existing = likeRepository.findByPostIdAndAccountId(postId, account.get().getId());
    if (existing.isPresent()) {
      likeRepository.delete(existing.get());
      return LikeResult.success("いいねを取り消しました。", false, likeRepository.countByPostId(postId));
    }
    T_PlayerPostLike like = new T_PlayerPostLike();
    like.setPostId(postId);
    like.setAccountId(account.get().getId());
    likeRepository.save(like);
    return LikeResult.success("いいねしました。", true, likeRepository.countByPostId(postId));
  }

  @Transactional
  public ActionResult deletePost(Authentication authentication, Long postId) {
    Optional<M_WebAccount> account = currentAccountService.current(authentication);
    if (account.isEmpty()) {
      return ActionResult.failure("投稿を削除するにはログインしてください。");
    }
    Optional<T_PlayerPost> post = postId == null ? Optional.empty() : postRepository.findById(postId);
    if (post.isEmpty()) {
      return ActionResult.failure("投稿が見つかりません。");
    }
    if (!account.get().getId().equals(post.get().getAccountId())) {
      return ActionResult.failure("自分の投稿だけ削除できます。");
    }
    likeRepository.deleteAllByPostId(postId);
    postRepository.delete(post.get());
    return ActionResult.success("投稿を削除しました。");
  }

  private PostView toView(T_PlayerPost post, M_WebAccount current) {
    String playerName = playerRepository.findById(post.getPlayerId())
        .map(M_Player::getPlayerName)
        .orElse("不明なプレイヤー");
    long likes = likeRepository.countByPostId(post.getId());
    boolean liked = current != null && likeRepository.findByPostIdAndAccountId(post.getId(), current.getId()).isPresent();
    boolean own = current != null && current.getId().equals(post.getAccountId());
    return new PostView(
        post.getId(), post.getPlayerId(), playerName, post.getBody(),
        displayTimeFormatter.format(post.getCreatedAt()), likes, liked, own);
  }

  public record PostView(
      Long id,
      Long playerId,
      String playerName,
      String body,
      String createdAt,
      long likeCount,
      boolean likedByCurrentAccount,
      boolean own) {
  }

  public record ActionResult(boolean success, String message) {
    static ActionResult success(String message) {
      return new ActionResult(true, message);
    }

    static ActionResult failure(String message) {
      return new ActionResult(false, message);
    }
  }

  public record LikeResult(
      boolean success,
      String message,
      boolean liked,
      long likeCount) {

    static LikeResult success(String message, boolean liked, long likeCount) {
      return new LikeResult(true, message, liked, likeCount);
    }

    static LikeResult failure(String message) {
      return new LikeResult(false, message, false, 0);
    }
  }
}
