package com.example.myfirst.service;

import com.example.myfirst.dto.response.NotificationResponse;

/**
 * 帖子互动服务 —— 处理点赞、评论、回复操作并生成通知
 * <p>
 * 当前使用模拟帖子数据（与 PostController 一致），
 * 后续引入 Post/Comment 实体后替换为数据库查询。
 */
public interface PostInteractionService {

    /**
     * 点赞帖子，为帖子所有者生成 LIKE 通知
     * @param actorId 操作者 ID
     * @param postId  帖子 ID
     * @return 创建的通知（若为自操作则返回 null）
     */
    NotificationResponse likePost(Long actorId, Long postId);

    /**
     * 评论帖子，为帖子所有者生成 COMMENT 通知
     * @param actorId 操作者 ID
     * @param postId  帖子 ID
     * @param content 评论内容
     * @return 创建的通知
     */
    NotificationResponse commentOnPost(Long actorId, Long postId, String content);

    /**
     * 回复评论，为评论所有者生成 REPLY 通知
     * @param actorId         操作者 ID
     * @param commentOwnerId  评论所有者 ID
     * @param commentId       评论 ID
     * @param content         回复内容
     * @return 创建的通知
     */
    NotificationResponse replyToComment(Long actorId, Long commentOwnerId, Long commentId, String content);
}
