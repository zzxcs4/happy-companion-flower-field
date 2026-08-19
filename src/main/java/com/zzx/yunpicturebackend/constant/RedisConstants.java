package com.zzx.yunpicturebackend.constant;

public interface RedisConstants {
    String BLOG_LIKED_KEY = "blog:liked:";
    String FEED_KEY = "feed:";
    String USER_SIGN_KEY="sign:";
    String SPACE_CAPACITY_GRACE_KEY = "space:capacity:grace:";
    
    // 推荐系统相关
    /** 网站访问 bitmap: visit:website:{userId}:{yyyyMM} */
    String VISIT_WEBSITE_KEY = "visit:website:";
    /** 社区访问 bitmap: visit:field:{userId}:{fieldId}:{yyyyMM} */
    String VISIT_FIELD_KEY = "visit:field:";
    /** 每日发帖计数器: daily:post:{userId}:{fieldId}:{yyyyMMdd} */
    String DAILY_POST_COUNT_KEY = "daily:post:";
    /** 每日评论计数器: daily:comment:{userId}:{fieldId}:{yyyyMMdd} */
    String DAILY_COMMENT_COUNT_KEY = "daily:comment:";
    /** 每日点赞计数器: daily:like:{userId}:{fieldId}:{yyyyMMdd} */
    String DAILY_LIKE_COUNT_KEY = "daily:like:";
    /** 连续签到奖励追踪: sign:streak:bonus:{userId}:{fieldId} */
    String SIGN_STREAK_BONUS_KEY = "sign:streak:bonus:";
    /** 连续未签周数: inactive:weeks:{userId} */
    String INACTIVE_WEEKS_KEY = "inactive:weeks:";
    /** 推荐结果缓存: recommend:fields:{userId}:{topN} */
    String RECOMMEND_FIELDS_KEY = "recommend:fields:";
}
