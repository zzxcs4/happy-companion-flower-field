<template>
  <div
    class="apple-page-container"
    :class="{
      'pc-layout-active': device === DEVICE_TYPE_ENUM.PC,
      'left-sider-collapsed': !showLeftSider,
      'right-chat-collapsed': !showRightChat
    }"
  >

    <!-- 最左侧独立组件栏 (仅在 PC 端侧边常驻) -->
    <SpaceDetailLeftSider
      v-if="device === DEVICE_TYPE_ENUM.PC && showLeftSider"
      :spaces="mySpaceList"
      :current-space-id="props.id"
      :active-nav="'space'"
      :used-storage-bytes="space.totalSize || space.usedStorage || 0"
      :max-storage-bytes="space.maxSize || space.maxStorage || (100 * 1024 * 1024 * 1024)"
      :is-unlimited="space.visibility === SPACE_VISIBILITY_ENUM.PUBLIC"
      :loading-more="spacesLoadingMore"
      :has-more="spacesHasMore"
      @select-space="handleSelectSpaceFromSider"
      @create-space="handleCreateSpaceFromSider"
      @open-settings="handleOpenSettingsFromSider"
      @select-nav="handleSelectNavFromSider"
      @load-more-spaces="fetchMoreSpaces"
    />

    <div id="spaceDetailPage"
         ref="spacePageRef"
         @scroll="handleSpacePageScroll"
         @touchstart="handleTouchStart"
         @touchmove="handleTouchMove"
         @touchend="handleTouchEnd"
         @touchcancel="handleTouchEnd">
      <!-- PC 端白底 Banner Header (PC 端下常驻显示，无 spaceId 时显示请选择空间引导) -->
      <div v-if="device === DEVICE_TYPE_ENUM.PC" class="yuemu-pc-banner-header">
        <!-- 1. 贯通顶行 Header (左侧侧边栏开关 + 搜索胶囊居中 + 右侧聊天室开关 & 用户头像) -->
        <div class="banner-search-row">
          <div class="top-header-left-actions">
            <button
              class="sidebar-toggle-btn"
              :class="{ 'is-collapsed': !showLeftSider }"
              :title="showLeftSider ? ($t('pages.spaceDetailPage.sidebar.collapseLeft') || '收起侧边栏') : ($t('pages.spaceDetailPage.sidebar.expandLeft') || '展开侧边栏')"
              @click.stop="toggleLeftSider"
            >
              <yuemu-icon name="sidebar-left" :active="!showLeftSider" :size="18" />
            </button>
          </div>

          <div class="pc-search-capsule" @click.stop="handleOpenSearchPopup">
            <yuemu-icon name="search" :size="14" class="search-icon" />
            <span class="search-placeholder">{{ $t('pages.spaceDetailPage.search.placeholder') }}</span>
            <span class="shortcut-k">⌘K</span>
          </div>

          <div class="top-header-right-user">
            <button
              v-if="props.id && space.spaceType === SPACE_TYPE_ENUM.TEAM && userSpaceStatus.isMember"
              class="sidebar-toggle-btn chat-toggle-btn"
              :class="{ 'is-collapsed': !showRightChat }"
              :title="showRightChat ? ($t('pages.spaceDetailPage.sidebar.collapseChat') || '收起聊天室') : ($t('pages.spaceDetailPage.sidebar.expandChat') || '展开聊天室')"
              @click.stop="toggleRightChat"
            >
              <yuemu-icon name="sidebar-right" :active="!showRightChat" :size="18" />
            </button>

            <button class="pc-header-share-btn" @click.stop="handleShareSpace" :title="$t('pages.spaceDetailPage.mobile.share')">
              <yuemu-icon name="share" :size="16" />
            </button>

            <div class="header-user-avatar-wrap" @click="router.push('/my')" :title="$t('pages.spaceDetailPage.banner.personalCenter')">
              <img :src="loginUserStore.loginUser?.userAvatar || getDefaultAvatar(loginUserStore.loginUser?.userName || $t('pages.spaceDetailPage.info.member'))" class="top-user-avatar-img"  alt="" />
            </div>
          </div>
        </div>

        <!-- 2. 第二行空间主体卡片 (像素级高保真复刻参考图) -->
        <div class="banner-main-card">
          <!-- 左侧大封面 -->
          <div class="banner-cover-wrap">
            <img v-if="props.id && space.spaceCover" :src="space.spaceCover" class="banner-cover-img"  alt="" />
            <div v-else class="banner-cover-fallback">
              <span>{{ props.id ? (space.spaceName || 'S').charAt(0).toUpperCase() : 'S' }}</span>
            </div>
          </div>

          <!-- 中间空间核心信息 -->
          <div class="banner-info-body">
            <div class="banner-space-title">{{ props.id ? (space?.spaceName || $t('pages.spaceDetailPage.info.spaceDetail')) : $t('pages.spaceDetailPage.empty.unselectedSpaceTitle') }}</div>
            <p class="banner-space-desc">{{ props.id ? (space?.spaceDesc || $t('pages.spaceDetailPage.banner.spaceDescFallback')) : $t('pages.spaceDetailPage.empty.unselectedSpaceDesc') }}</p>

            <div class="banner-members-row" v-if="space?.spaceType === SPACE_TYPE_ENUM.TEAM">
              <div class="avatar-stack">
                <img
                  v-for="(m, idx) in topMembers"
                  :key="m.id || idx"
                  :src="m.avatar"
                  class="stack-avatar"
                  :title="m.name"
                  @click="handleUserClick(m)"
                  style="cursor: pointer;"
                 alt="" />
              </div>
              <span class="member-count-text">{{ $t('pages.spaceDetailPage.info.memberCount', { count: memberTotalCount }) }}</span>
            </div>
            <div class="banner-members-row" v-else>
              <span class="private-tag-badge"><yuemu-icon name="lock" :size="11" /> {{ $t('pages.spaceDetailPage.info.personalPrivate') }}</span>
            </div>
          </div>

        </div>
      </div>

      <!-- 移动端专属电影级沉浸大图 Banner Header (移动端下常驻显示，无空间 id 时提供“请选择空间 ▾”全功能切换抽屉) -->
      <div v-else-if="device !== DEVICE_TYPE_ENUM.PC" class="mobile-space-immersive-banner">
        <img :src="space.spaceCover || 'https://static.yuemutuku.com/post/1866450683272450049/2026-07-28_wgPlyGoK33wM5BZg.webp'" class="banner-bg-img"  alt="" />
        <div class="banner-mask-overlay"></div>

        <!-- 顶栏操作行 (左侧纯白小于号返回 + 中间 1:1 MyPage 样式刷新圈 + 右侧纯白图标) -->
        <div class="mobile-top-bar">
          <button class="mobile-back-icon" @click="handleMobileBack" :title="$t('pages.spaceDetailPage.mobile.back')">
            <yuemu-icon name="chevron-right" :size="20" style="transform: rotate(180deg);" />
          </button>

          <!-- 沉浸顶栏下拉刷新指示器 (包含箭头/Spinner + 状态文案，绝对定位零高度抖动) -->
          <div v-if="refreshDistance > 0 || isRefreshing" class="mypage-style-top-spinner">
            <template v-if="isRefreshing">
              <span class="yuemu-spinner"></span>
              <span class="refresh-tip-text">{{ $t('pages.spaceDetailPage.refresh.refreshing') }}</span>
            </template>
            <template v-else-if="refreshDistance >= refreshThreshold">
              <yuemu-icon name="chevron-up" :size="14" class="refresh-arrow-icon" />
              <span class="refresh-tip-text">{{ $t('pages.spaceDetailPage.refresh.release') }}</span>
            </template>
            <template v-else>
              <yuemu-icon name="chevron-down" :size="14" class="refresh-arrow-icon" />
              <span class="refresh-tip-text">{{ $t('pages.spaceDetailPage.refresh.pull') }}</span>
            </template>
          </div>

          <div class="mobile-top-right-actions">
            <button class="mobile-icon-btn" @click="handleOpenSearchPopup" :title="$t('pages.spaceDetailPage.mobile.search')">
              <yuemu-icon name="search" :size="18" />
            </button>
            <button class="mobile-icon-btn" @click="handleShareSpace" :title="$t('pages.spaceDetailPage.mobile.share')">
              <yuemu-icon name="share" :size="16" />
            </button>
          </div>
        </div>

        <!-- 空间核心信息 (全白字层) -->
        <div class="mobile-banner-content">
          <div class="mobile-space-title-btn" @click="showMobileSpaceSheet = true" :title="$t('pages.spaceDetailPage.mobile.switchSpaceHint')">
            <h2 class="mobile-space-title">
              <span>{{ props.id ? (space.spaceName || $t('pages.spaceDetailPage.info.spaceDetail')) : $t('pages.spaceDetailPage.empty.unselectedSpaceTitle') }}</span>
              <yuemu-icon name="chevron-down" :size="16" class="title-arrow-icon" />
            </h2>
          </div>

          <p class="mobile-space-desc">{{ props.id ? (space.spaceDesc || $t('pages.spaceDetailPage.banner.spaceDescFallback')) : $t('pages.spaceDetailPage.empty.unselectedSpaceDesc') }}</p>

          <div class="mobile-members-avatar-row" v-if="space.spaceType === SPACE_TYPE_ENUM.TEAM">
            <div class="mobile-avatar-stack">
              <img
                v-for="(m, idx) in topMembers"
                :key="m.id || idx"
                :src="m.avatar"
                class="mobile-stack-avatar"
                @click="handleUserClick(m)"
               alt="" />
            </div>
            <span class="mobile-member-count">{{ $t('pages.spaceDetailPage.info.memberCount', { count: memberTotalCount }) }}</span>
          </div>
        </div>
      </div>

      <!-- 3. Sub-Nav 选项卡（在 PC 和移动端统一开放渲染） -->
      <div v-if="props.id" class="banner-sub-tabs" :class="{ 'is-sticky-active': isHeaderSticky }">
        <div class="sub-tabs-left">
          <div
            v-for="tab in spaceNavTabs"
            :key="tab.key"
            class="tab-item"
            :class="{ 'is-active': activeNavTab === tab.key, 'is-disabled': !userSpaceStatus.isMember && !isPublicPreview }"
            @click="handleTabClick(tab.key)"
          >
            <span class="tab-label">{{ tab.label }}</span>
            <div class="active-indicator" v-if="activeNavTab === tab.key"></div>
          </div>
        </div>

        <!-- 吸顶时右侧优雅浮现的搜索胶囊 -->
        <div class="sticky-right-search" v-if="isHeaderSticky" @click="handleOpenSearchPopup">
          <yuemu-icon name="search" :size="14" class="search-icon" />
          <span class="search-text">{{ $t('pages.spaceDetailPage.banner.search') }}</span>
          <span class="shortcut-k">⌘K</span>
        </div>
      </div>

      <div ref="contentWrapRef" class="content-wrap" @scroll="checkScrollBottom">
        <!-- 无空间选中居中卡片 (仅 PC 端未选空间时显示) -->
        <div v-if="!props.id && device === DEVICE_TYPE_ENUM.PC" class="permission-denied-state">
          <div class="denied-content">
            <div class="lock-icon-wrap">
              <yuemu-icon name="images" :size="28" />
            </div>
            <h2 class="denied-title">{{ $t('pages.spaceDetailPage.empty.unselectedSpaceTitle') }}</h2>
            <p class="denied-desc">{{ $t('pages.spaceDetailPage.empty.unselectedSpaceDesc') }}</p>
            <button class="apple-primary-btn" @click="handleNavigate({ path: '/add_space' })">
              <yuemu-icon name="plus" :size="14" />
              <span>{{ $t('pages.spaceDetailPage.overview.createSpace') }}</span>
            </button>
          </div>
        </div>

        <!-- 权限阻断：RESTRICTED/HIDDEN 团队空间 or 私有空间（非成员） -->
        <div v-if="props.id && !isStatusLoading && !userSpaceStatus.isMember && !isPublicPreview" class="permission-denied-state">
          <div class="denied-content">
            <div class="lock-icon-wrap"><yuemu-icon name="lock" :size="28" /></div>
            <h2 class="denied-title">
              {{ space.visibility === SPACE_VISIBILITY_ENUM.HIDDEN ? $t('pages.spaceDetailPage.privateAccess.hiddenTitle') : $t('pages.spaceDetailPage.privateAccess.title') }}
            </h2>
            <p class="denied-desc">
              {{ space.visibility === SPACE_VISIBILITY_ENUM.HIDDEN ? $t('pages.spaceDetailPage.privateAccess.hiddenDesc') : $t('pages.spaceDetailPage.privateAccess.desc') }}
            </p>
            <button
              v-if="space.spaceType !== SPACE_TYPE_ENUM.PRIVATE"
              class="apple-primary-btn"
              @click="applyToJoinSpace"
              :class="{ 'is-pending': userSpaceStatus.hasPending }"
            >
              <yuemu-icon v-if="!userSpaceStatus.hasPending" name="user" :size="14" />
              <span v-else class="yuemu-spinner"></span>
              <span>{{ userSpaceStatus.hasPending ? $t('pages.spaceDetailPage.privateAccess.pendingBtn') : $t('pages.spaceDetailPage.privateAccess.applyBtn') }}</span>
            </button>
          </div>
        </div>

        <!-- 超额用量警告条：空间已使用量超过当前配额（会员到期降级导致） -->
        <div v-if="isOverQuota && !isStatusLoading && (userSpaceStatus.isMember || isPublicPreview)" class="over-quota-warning-banner">
          <yuemu-icon name="warning" :size="16" />
          <span>
            {{ $t('pages.spaceDetailPage.overQuota.used') }}
            <strong>{{ formatBytes((space.usedStorage || 0) * 1024 * 1024) }}</strong>
            &nbsp;|&nbsp;
            {{ $t('pages.spaceDetailPage.overQuota.quota') }}
            <strong>{{ formatBytes((space.maxStorage || 0) * 1024 * 1024) }}</strong>
            &nbsp;|&nbsp;
            {{ $t('pages.spaceDetailPage.overQuota.over') }}
            <strong>{{ formatBytes(((space.usedStorage || 0) - (space.maxStorage || 0)) * 1024 * 1024) }}</strong>
            。{{ $t('pages.spaceDetailPage.overQuota.desc') }}
          </span>
        </div>

        <!-- 预览模式提示条：PUBLIC 团队空间（非成员） -->
        <div v-if="isPublicPreview && !isStatusLoading" class="public-preview-banner">
          <span>{{ $t('pages.spaceDetailPage.privateAccess.previewDesc') }}</span>
          <button
            class="preview-apply-btn"
            @click="applyToJoinSpace"
            :class="{ 'is-pending': userSpaceStatus.hasPending }"
          >
            <yuemu-icon v-if="!userSpaceStatus.hasPending" name="user" :size="14" />
            <span v-else class="yuemu-spinner"></span>
            <span>{{ userSpaceStatus.hasPending ? $t('pages.spaceDetailPage.privateAccess.pendingBtn') : $t('pages.spaceDetailPage.privateAccess.previewApplyBtn') }}</span>
          </button>
        </div>

        <!-- 空间公告条：单行贯通排版 -->
        <div
          v-if="props.id && space.announcement && (userSpaceStatus.isMember || isPublicPreview) && !isStatusLoading"
          class="space-announcement-bar"
        >
          <div class="announcement-icon"><yuemu-icon name="megaphone" :size="16" /></div>
          <div class="announcement-text-single" :title="space.announcement">{{ space.announcement }}</div>
          <div class="announcement-time-single" v-if="space.announcementTime">
            {{ formatTime(space.announcementTime) }}
          </div>
        </div>

        <template v-if="userSpaceStatus.isMember || isPublicPreview">
          <div v-if="activeNavTab === 'dynamics'">
            <SpaceFeedList
              :feedList="spacePosts"
              :loading="spacePostsLoading"
              :loadingMore="spacePostsLoadingMore"
              :hasMore="spacePostsHasMore"
              :canPublish="canPublishSpacePost"
              @publish="openSpacePostEditor"
              @loadMore="loadMoreSpacePosts"
              @itemClick="openSpaceFeedItem"
            />
          </div>
          <!-- 2. 成员 Tab 专属：1:1 截图极简成员列表面板 -->
          <div v-if="activeNavTab === 'members'" class="yuemu-members-tab-panel">
          <!-- 成员 Header 行：成员 (4) ----------------------------- 活跃成员榜 | 管理 -->
          <div class="members-panel-header">
            <div class="header-left-title">
              <span class="title-text">{{ $t('pages.spaceDetailPage.members.title') }}</span>
              <span class="count-num">({{ memberTotalCount }})</span>
            </div>
            <div class="header-right-actions">
              <button class="header-rank-btn" @click="openRankModal">
                <yuemu-icon name="rank" :size="14" class="rank-star-icon" />
                <span>{{ $t('pages.spaceDetailPage.activeMembers.title', '活跃成员榜') }}</span>
              </button>
              <button class="header-manage-btn" v-if="canManageSpace" @click="handleManageMembers">
                {{ $t('pages.spaceDetailPage.members.manage') }}
              </button>
            </div>
          </div>

          <!-- 首次加载状态 -->
          <div v-if="membersLoading && (!paginatedMembers || paginatedMembers.length === 0)" class="activities-loading-state">
            <a-spin size="large" :tip="$t('pages.spaceDetailPage.status.loadingMembers')" />
          </div>

          <!-- 高級双列成员卡片阵列 (一排两列) -->
          <div v-if="paginatedMembers && paginatedMembers.length > 0" class="members-cards-grid">
            <div
              v-for="item in paginatedMembers"
              :key="item.id || item.userId"
              class="member-grid-card"
              @click="handleUserClick(item)"
            >
              <div class="card-avatar-wrap">
                <img :src="item.user?.userAvatar || item.userAvatar || getDefaultAvatar(item.user?.userName || item.userName || $t('pages.spaceDetailPage.info.member'))" class="card-user-avatar"  alt="" />
              </div>

              <div class="card-user-info">
                <div class="card-name-row">
                  <h4 class="card-user-name">{{ item.user?.userName || item.userName || $t('pages.spaceDetailPage.members.spaceMember') }}</h4>
                  <span class="card-role-badge" :class="item.spaceRole === 'admin' ? 'is-admin' : 'is-member'">
                    {{ String(item.userId) === String(space?.userId) ? $t('pages.spaceDetailPage.members.roleCreator') : getRoleSimpleName(item.spaceRole, false) }}
                  </span>
                </div>
                <p class="card-join-time">{{ $t('pages.spaceDetailPage.info.joinedPrefix') }}{{ formatTime(item.createTime || space?.createTime) }}</p>
              </div>

              <div class="card-more-btn" v-if="canManageSpaceUser" @click.stop="handleManageMembers" :title="$t('pages.spaceDetailPage.members.manageTitle')">
                <yuemu-icon name="more" :size="18" />
              </div>
            </div>
          </div>

            <!-- 探底加载提示 -->
            <div v-if="membersLoadingMore" class="space-list-loading-more">
              <span class="yuemu-spinner"></span> <span>{{ $t('pages.spaceDetailPage.status.loadingMoreMembers') }}</span>
            </div>
            <div v-if="!membersHasMore && paginatedMembers.length > 0 && !membersLoading && !membersLoadingMore" class="space-list-no-more">
              {{ $t('pages.spaceDetailPage.status.allMembersLoaded') }}
            </div>

          <!-- 空数据状态 -->
          <div v-else class="activities-empty-state">
            <div class="empty-icon-circle">
              <yuemu-icon name="users" :size="22" />
            </div>
            <h4 class="empty-title">{{ $t('pages.spaceDetailPage.empty.noMembers') }}</h4>
            <p class="empty-desc">{{ $t('pages.spaceDetailPage.empty.noMembersDesc') }}</p>
          </div>
        </div>

        <!-- 1. 活动 Tab 专属：Apple Today 级内容驱动网格卡片 (App Store Grid) -->
        <div v-else-if="props.id && activeNavTab === 'activities'" class="yuemu-apple-activities-wrap">
          <!-- 活动 Header 行：精彩活动 ----------------------------- 管理 -->
          <div class="members-panel-header">
            <div class="header-left-title">
              <span class="title-text">{{ $t('pages.spaceDetailPage.activities.title') }}</span>
              <span class="count-num" v-if="activities && activities.length > 0">({{ activities.length }})</span>
            </div>
            <button class="header-manage-btn" v-if="canManageSpace" @click="handleManageActivities">
              {{ $t('pages.spaceDetailPage.members.manage') }}
            </button>
          </div>

          <div v-if="activitiesLoading" class="activities-loading-state">
            <a-spin size="large" :tip="$t('pages.spaceDetailPage.activities.loading')" />
          </div>

          <div v-else-if="activities && activities.length > 0" class="apple-activity-grid">
            <div
              v-for="act in activities"
              :key="act.id"
              class="apple-activity-card"
              @click="handleActivityClick(act.id)"
            >
              <!-- 上方 16:9 高清大图封面 + 极简 Apple Status Badge -->
              <div class="card-cover-wrapper">
                <img :src="act.coverUrl || 'https://static.yuemutuku.com/post/1866450683272450049/2026-07-28_wgPlyGoK33wM5BZg.webp'" class="card-cover-img"  alt="" />
                <div class="card-status-tag" :class="act.isExpired ? 'is-expired' : 'is-live'">
                  <span class="status-dot"></span>
                  <span class="status-text">{{ act.isExpired ? $t('pages.spaceDetailPage.activities.expired') : $t('pages.spaceDetailPage.activities.live') }}</span>
                </div>
              </div>

              <!-- 卡片内容主体 -->
              <div class="card-content-body">
                <div class="card-header-info">
                  <h3 class="card-title">{{ act.title }}</h3>
                  <p class="card-subtitle">{{ act.content || act.description || $t('pages.spaceDetailPage.activities.descFallback') }}</p>
                </div>

                <!-- 用户视角极简数据 Pill 行 -->
                <div class="card-meta-pills">
                  <span class="meta-pill">
                    <yuemu-icon name="camera" :size="11" /> {{ act.submissionCount || 0 }} {{ $t('pages.spaceDetailPage.activities.submissions') }}
                  </span>
                  <span class="meta-pill">
                    <yuemu-icon name="users" :size="11" /> {{ act.voteCount || 0 }} {{ $t('pages.spaceDetailPage.activities.participants') }}
                  </span>
                  <span class="meta-pill" v-if="act.expireTime">
                    <yuemu-icon name="clock" :size="11" /> {{ formatTime(act.expireTime) }}
                  </span>
                </div>

                <!-- 底部分割线与 Apple 风格 CTA 按钮 -->
                <div class="card-action-bar">
                  <div class="author-info">
                    <span class="author-label">{{ $t('pages.spaceDetailPage.activities.organizer') }}</span>
                    <span class="author-name">@{{ act.user?.userName || $t('pages.spaceDetailPage.members.roleAdmin') }}</span>
                  </div>
                  <button v-if="canUploadPicture" class="cta-join-btn" @click.stop="handleActivityClick(act.id)">
                    <span>{{ $t('pages.spaceDetailPage.activities.joinBtn') }}</span>
                    <yuemu-icon name="chevron-right" :size="14" />
                  </button>
                  <span v-else class="cta-join-btn disabled" @click.stop>{{ $t('pages.spaceDetailPage.activities.joinBtn') }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 活动触底加载提示 -->
          <div v-if="activitiesLoadingMore" class="space-list-loading-more">
            <span class="yuemu-spinner"></span> <span>{{ $t('pages.spaceDetailPage.activities.loadingMore') }}</span>
          </div>
          <div v-if="!activitiesHasMore && activities.length > 0 && !activitiesLoading && !activitiesLoadingMore" class="space-list-no-more">
            {{ $t('pages.spaceDetailPage.activities.allLoaded') }}
          </div>

          <!-- 空数据状态 -->
          <div v-else-if="(!activities || activities.length === 0) && !activitiesLoading" class="apple-empty-activities">
            <div class="empty-sparkle-box">
              <yuemu-icon name="magic" :size="22" />
            </div>
            <h4 class="empty-title">{{ $t('pages.spaceDetailPage.activities.emptyTitle') }}</h4>
            <p class="empty-desc">{{ $t('pages.spaceDetailPage.activities.emptyDesc') }}</p>
          </div>
        </div>
        <!-- 2. 设置 Tab 专属：极简纯粹功能入口 (点击分析跳转，点击编辑开弹窗) -->
        <div v-else-if="activeNavTab === 'settings'" class="yuemu-settings-pure-wrap">
          <div class="settings-options-card">
            <!-- 选项 1: 空间数据分析 -->
            <div class="settings-option-item" @click="handleGoToAnalyze" v-if="canManageSpace">
              <div class="option-left">
                <div class="option-icon-box bg-blue">
                  <yuemu-icon name="rank" :size="16" />
                </div>
                <div class="option-text-info">
                  <h4 class="option-title">{{ $t('pages.spaceDetailPage.settings.analyzeTitle') }}</h4>
                  <p class="option-desc">{{ $t('pages.spaceDetailPage.settings.analyzeDesc') }}</p>
                </div>
              </div>
              <div class="option-right">
                <yuemu-icon name="chevron-right" :size="16" class="arrow-icon" />
              </div>
            </div>

            <div class="option-divider" v-if="canManageSpace"></div>

            <div class="settings-option-item" @click="handleManageActivities" v-if="canManageActivity && space.spaceType === SPACE_TYPE_ENUM.TEAM">
              <div class="option-left">
                <div class="option-icon-box bg-green"><yuemu-icon name="clock" :size="16" /></div>
                <div class="option-text-info">
                  <h4 class="option-title">{{ $t('pages.spaceDetailPage.settings.activityManageTitle') }}</h4>
                  <p class="option-desc">{{ $t('pages.spaceDetailPage.settings.activityManageDesc') }}</p>
                </div>
              </div>
              <div class="option-right"><yuemu-icon name="chevron-right" :size="16" class="arrow-icon" /></div>
            </div>

            <div class="option-divider" v-if="canManageActivity && space.spaceType === SPACE_TYPE_ENUM.TEAM"></div>

            <!-- 选项: 空间公告（入口移至设置，仅 admin 可见） -->
            <div class="settings-option-item" @click="openAnnouncementEditor" v-if="canManageSpace">
              <div class="option-left">
                <div class="option-icon-box bg-purple"><yuemu-icon name="megaphone" :size="16" /></div>
                <div class="option-text-info">
                  <h4 class="option-title">{{ $t('pages.spaceDetailPage.settings.announcementTitle') }}</h4>
                  <p class="option-desc">{{ $t('pages.spaceDetailPage.settings.announcementDesc') }}</p>
                </div>
              </div>
              <div class="option-right"><yuemu-icon name="chevron-right" :size="16" class="arrow-icon" /></div>
            </div>

            <div class="option-divider" v-if="canManageSpace"></div>

            <!-- 选项: 审核动态流（仅 admin 可见） -->
            <div class="settings-option-item" @click="handleManageReview" v-if="canManageSpace">
              <div class="option-left">
                <div class="option-icon-box bg-orange"><yuemu-icon name="checklist" :size="16" /></div>
                <div class="option-text-info">
                  <h4 class="option-title">{{ $t('pages.spaceDetailPage.settings.reviewTitle') }}</h4>
                  <p class="option-desc">{{ $t('pages.spaceDetailPage.settings.reviewDesc') }}</p>
                </div>
              </div>
              <div class="option-right"><yuemu-icon name="chevron-right" :size="16" class="arrow-icon" /></div>
            </div>

            <div class="option-divider" v-if="canManageSpace"></div>

            <!-- 选项 2: 编辑空间信息 -->
            <div class="settings-option-item" @click="openEditSpacePage" v-if="isSpaceOwner">
              <div class="option-left">
                <div class="option-icon-box bg-green">
                  <yuemu-icon name="edit" :size="16" />
                </div>
                <div class="option-text-info">
                  <h4 class="option-title">{{ $t('pages.spaceDetailPage.settings.editTitle') }}</h4>
                  <p class="option-desc">{{ $t('pages.spaceDetailPage.settings.editDesc') }}</p>
                </div>
              </div>
              <div class="option-right">
                <yuemu-icon name="chevron-right" :size="16" class="arrow-icon" />
              </div>
            </div>
          </div>
        </div>



        <div v-else-if="props.id && activeNavTab === 'pictures'" class="pictures-tab-pure-wrap">
          <BigPictureList
            v-if="isStatusLoading || loading || dataList.length > 0"
            :dataList="dataList"
            :loading="isStatusLoading || loading"
            :showOp="true"
            :onReload="fetchData"
            :isMyPosts="false"
          />



          <!-- 触底加载与没有更多状态 -->
          <div v-if="loadingMore" class="space-list-loading-more">
            <span class="yuemu-spinner"></span> <span>{{ $t('pages.spaceDetailPage.status.loadingMore') }}</span>
          </div>
          <div v-if="!hasMore && dataList.length > 0 && !loading && !loadingMore" class="space-list-no-more">
            {{ $t('pages.spaceDetailPage.status.noMore') }}
          </div>

          <div v-else-if="(userSpaceStatus.isMember || isPublicPreview) && dataList.length === 0 && !loading && !isStatusLoading" class="apple-empty-state">
            <div class="empty-icon-wrap">
              <yuemu-icon name="images" :size="32" />
            </div>
            <h3 class="empty-title">{{ $t('pages.spaceDetailPage.empty.title') }}</h3>
            <p class="empty-desc">{{ canUploadPicture ? $t('pages.spaceDetailPage.empty.userHint') : $t('pages.spaceDetailPage.empty.adminHint') }}</p>
          </div>
        </div>
        </template>
      </div>

      
      <input type="file" accept="image/*" style="display: none;" />

      <div v-if="showQuitConfirmDialog" class="apple-overlay" @click="cancelQuit">
        <div class="apple-dialog" @click.stop>
          <h3 class="dialog-title">{{ $t('pages.spaceDetailPage.quitDialog.title') }}</h3>
          <p class="dialog-desc">{{ $t('pages.spaceDetailPage.quitDialog.desc') }}</p>
          <div class="dialog-actions">
            <button class="dialog-btn cancel" @click="cancelQuit">{{ $t('pages.spaceDetailPage.editModal.cancel') }}</button>
            <button class="dialog-btn danger" @click="confirmQuit">{{ $t('pages.spaceDetailPage.quitDialog.title') }}</button>
          </div>
        </div>
      </div>

      <div v-if="showJoinSpaceDialog" class="apple-overlay" @click="cancelJoin">
        <div class="apple-dialog" @click.stop>
          <h3 class="dialog-title">{{ $t('pages.spaceDetailPage.privateAccess.dialogTitle') }}</h3>
          <p class="dialog-desc">{{ $t('pages.spaceDetailPage.privateAccess.dialogDesc') }}</p>
          <div class="dialog-actions">
            <button class="dialog-btn cancel" @click="cancelJoin">{{ $t('pages.spaceDetailPage.privateAccess.cancelJoin') }}</button>
            <button class="dialog-btn confirm" @click="joinSpace">{{ $t('pages.spaceDetailPage.privateAccess.confirmJoin') }}</button>
          </div>
        </div>
      </div>

      <input ref="fileInputRef" type="file" accept="image/*" @change="handleFileChange" style="display: none;" />

      <!-- 全屏独立搜索页 -->
      <div v-if="showSearchPopup" class="full-search-backdrop" @click.self="handleCloseSearchPopup">
        <div class="full-search-popup" :class="{ 'is-desktop': !isMobile, 'is-mobile': isMobile }" @click.stop>
          <div class="drawer-drag-bar" v-if="isMobile" @click="handleCloseSearchPopup"></div>
        <div class="search-popup-header">
          <div class="search-input-wrap">
            <yuemu-icon name="search" :size="16" />
            <input type="text" v-model="spaceSearchKeyword" :placeholder="$t('pages.spaceDetailPage.search.inputPlaceholder')" @keyup.enter="doPopupSearch" />
          </div>
          <button class="search-btn" @click="doPopupSearch">{{ $t('pages.spaceDetailPage.search.btn') }}</button>
          <button class="popup-close-icon-btn" @click.stop.prevent="handleCloseSearchPopup" :title="$t('common.close')">
            <yuemu-icon name="close" :size="18" />
          </button>
        </div>
        
        <div class="search-popup-body">
          <div class="space-search-tabs">
            <button v-for="item in spaceSearchTabs" :key="item.key" class="space-search-tab" :class="{ active: spaceSearchType === item.key }" @click="selectSpaceSearchType(item.key)">{{ item.label }}</button>
          </div>

          <div v-if="spaceSearchType === 'picture'" class="advanced-tools-grid">
            <div class="tool-card" @click="runSemanticSearch">
              <div class="tool-icon bg-gradient-violet"><yuemu-icon name="magic" :size="20" /></div>
              <div class="tool-text">
                <h4>{{ $t('pages.spaceDetailPage.search.semanticTitle') }}</h4>
                <p>{{ $t('pages.spaceDetailPage.search.semanticDesc') }}</p>
              </div>
              <yuemu-icon name="chevron-right" :size="14" class="tool-arrow" />
            </div>
            <div class="tool-card" @click="openColorPicker">
              <div class="tool-icon bg-gradient-pink"><yuemu-icon name="palette" :size="20" /></div>
              <div class="tool-text">
                <h4>{{ $t('pages.spaceDetailPage.search.colorTitle') }}</h4>
                <p>{{ $t('pages.spaceDetailPage.search.colorDesc') }}</p>
              </div>
              <yuemu-icon name="chevron-right" :size="14" class="tool-arrow" />
            </div>
            <div class="tool-card" @click="cameraInputRef?.click()">
              <div class="tool-icon bg-gradient-blue"><yuemu-icon name="camera" :size="20" /></div>
              <div class="tool-text">
                <h4>{{ $t('pages.spaceDetailPage.search.imageTitle') }}</h4>
                <p>{{ $t('pages.spaceDetailPage.search.imageDesc') }}</p>
              </div>
              <yuemu-icon name="chevron-right" :size="14" class="tool-arrow" />
            </div>
          </div>

          <!-- 经典高奢 12 色系 上下两层色盘 -->
          <div v-if="showColorPickerInPopup" class="inline-color-picker">
            <div class="picker-header">
              <span class="picker-title"><yuemu-icon name="palette" :size="14" /> {{ $t('pages.spaceDetailPage.search.colorPickerTitle') }}</span>
              <button class="close-picker" @click="showColorPickerInPopup = false"><yuemu-icon name="close" :size="14" /></button>
            </div>
            
            <div class="picker-body">
              <!-- 【上层】12 个 Preset 圆球 (2 行 x 6 个, 左右两端与下面 100% 齐平对齐) -->
              <div class="preset-swatches-grid">
                <div class="preset-swatches-row">
                  <div
                    v-for="c in presetColors.slice(0, 6)"
                    :key="c.value"
                    class="swatch-circle-item"
                    :style="{ backgroundColor: c.value }"
                    :title="c.name"
                    @click="selectPresetColor(c.value)"
                  >
                    <yuemu-icon v-if="selectedHexColor.toUpperCase() === c.value.toUpperCase()" name="check" :size="12" class="check-icon" :customColor="c.value === '#FFFFFF' ? '#000' : '#fff'" />
                  </div>
                </div>
                <div class="preset-swatches-row">
                  <div
                    v-for="c in presetColors.slice(6, 12)"
                    :key="c.value"
                    class="swatch-circle-item"
                    :style="{ backgroundColor: c.value }"
                    :title="c.name"
                    @click="selectPresetColor(c.value)"
                  >
                    <yuemu-icon v-if="selectedHexColor.toUpperCase() === c.value.toUpperCase()" name="check" :size="12" class="check-icon" :customColor="c.value === '#FFFFFF' ? '#000' : '#fff'" />
                  </div>
                </div>
              </div>

              <!-- 【下层】自定义 Hex 选色组 (胶囊 + 文本框 + 搜索按钮) -->
              <div class="custom-color-row">
                <div class="native-color-picker-wrap" :style="{ backgroundColor: selectedHexColor }">
                  <input type="color" v-model="selectedHexColor" class="native-color-input" />
                </div>
                <input type="text" v-model="selectedHexColor" class="hex-text-input" :placeholder="$t('pages.spaceDetailPage.search.hexPlaceholder')" />
                <button class="confirm-color-btn" @click="confirmCustomColorSearch">{{ $t('pages.spaceDetailPage.search.btn') }}</button>
              </div>
            </div>
          </div>

          <div v-if="spaceSearchLoading" class="space-search-loading"><a-spin size="large" /></div>
          <template v-else-if="spaceSearchHasRun">
            <div v-if="spaceSearchResults && spaceSearchResults.length > 0" class="space-search-results-waterfall">
              <SpaceFeedList
                :feedList="spaceSearchResults"
                :loading="spaceSearchLoading"
                :loadingMore="spaceSearchLoadingMore"
                :hasMore="spaceSearchHasMore"
                @loadMore="searchSpaceContent(false)"
                @itemClick="openSearchResult"
              />
            </div>
            <div v-else class="space-search-empty">
              <div class="empty-icon-circle">
                <yuemu-icon name="search" :size="22" />
              </div>
              <h4 class="empty-title">{{ $t('pages.spaceDetailPage.search.emptyResultTitle') }}</h4>
              <p class="empty-desc">{{ $t('pages.spaceDetailPage.search.emptyResultDesc') }}</p>
            </div>
          </template>
          <!-- 未搜索状态探索引导盒 (告别死寂空白) -->
          <div v-else class="search-initial-hint">
            <div class="empty-icon-circle">
              <yuemu-icon name="compass" :size="22" />
            </div>
            <h4 class="empty-title">{{ $t('pages.spaceDetailPage.search.initialHintTitle') }}</h4>
            <p class="empty-desc">{{ $t('pages.spaceDetailPage.search.initialHintDesc') }}</p>
          </div>
        </div>
        <input type="file" ref="cameraInputRef" accept="image/*" style="display: none" @change="handleCameraUpload" capture="environment">
        </div>
      </div>

      <!-- 统一上传进度胶囊 -->
      <UploadProgressOverlay
        :visible="isUploadActive"
        :progress="uploadProgress"
        :phase="uploadPhase"
        :errorMessage="uploadErrorMessage"
        :fileName="uploadFileLabel"
        :fixed="true"
        :blocking="false"
      />

      <!-- 空间公告编辑弹窗 (纯 div 原生搭建，全量支持 Dark Mode) -->
      <div v-if="announcementModalOpen" class="apple-overlay" @click.self="closeAnnouncementEditor">
        <div class="yuemu-custom-modal announcement-custom-modal" @click.stop>
          <div class="yuemu-custom-modal-header">
            <h3 class="yuemu-custom-modal-title">{{ $t('pages.spaceDetailPage.announcement.title') }}</h3>
            <button class="yuemu-custom-modal-close" @click="closeAnnouncementEditor">
              <yuemu-icon name="close" :size="18" />
            </button>
          </div>
          <div class="yuemu-custom-modal-body">
            <textarea
              v-model="announcementDraft"
              class="yuemu-custom-textarea custom-scroll"
              :placeholder="$t('pages.spaceDetailPage.announcement.placeholder')"
              maxlength="2000"
              rows="5"
            ></textarea>
            <div class="announcement-modal-tip">
              <yuemu-icon name="info" :size="14" />
              {{ $t('pages.spaceDetailPage.announcement.modalTip') }}
            </div>
          </div>
          <div class="yuemu-custom-modal-footer">
            <button class="yuemu-modal-btn cancel" @click="closeAnnouncementEditor">
              {{ $t('pages.spaceDetailPage.announcement.cancel') }}
            </button>
            <button class="yuemu-modal-btn confirm" :disabled="announcementSaving" @click="saveAnnouncement">
              <span v-if="announcementSaving" class="yuemu-spinner"></span>
              <span>{{ $t('pages.spaceDetailPage.announcement.save') }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- PC 端最右侧独立空间聊天栏 -->
    <SpaceDetailRightChat
      v-if="props.id && space.spaceType === SPACE_TYPE_ENUM.TEAM && userSpaceStatus.isMember && device === DEVICE_TYPE_ENUM.PC && showRightChat"
      :key="`chat-${props.id}`"
      :space-id="props.id"
      :space="space"
      @toggle-collapse="toggleRightChat"
    />

    <!-- 移动端 Apple Maps 风格底部 3 态悬浮聊天 Handle 抽屉 (1:1 高保真截图胶囊卡片) -->
    <div
      v-if="props.id && space.spaceType === SPACE_TYPE_ENUM.TEAM && userSpaceStatus.isMember && device !== DEVICE_TYPE_ENUM.PC"
      class="mobile-chat-apple-sheet"
      :class="[`is-${mobileChatSheetState}`]"
    >
      <!-- 1. 顶部固定 Handle 抓手胶囊栏 (1:1 高保真截图样式) -->
      <div
        class="sheet-handle-capsule-bar"
        @click="toggleMobileChatSheetState"
        @touchstart="handleChatSheetTouchStart"
        @touchmove="handleChatSheetTouchMove"
        @touchend="handleChatSheetTouchEnd"
      >
        <div class="handle-bar-content">
          <div class="preview-left">
            <!-- 1:1 线条风对话气泡 Icon -->
            <div class="chat-bubble-box">
              <yuemu-icon name="chat" :size="18" class="bubble-icon" />
            </div>
            
            <!-- 1:1 高保真昵称: 内容 -->
            <div class="preview-text-wrap" v-if="latestChatMessageInfo.sender">
              <span class="sender-name">{{ latestChatMessageInfo.sender }}</span>
              <span class="msg-colon">: </span>
              <span class="msg-content">{{ latestChatMessageInfo.content }}</span>
            </div>
            <div class="preview-text-wrap" v-else>
              <span class="placeholder-text">{{ $t('pages.spaceDetailPage.chat.placeholder') }}</span>
            </div>
          </div>

          <!-- 右侧 1:1 展开 Chevron 箭头 -->
          <div class="preview-right">
            <yuemu-icon :name="mobileChatSheetState === 'expanded' ? 'chevron-down' : 'chevron-up'" :size="18" />
          </div>
        </div>
      </div>

      <!-- 2. 抽屉内部嵌入聊天室组件 -->
      <div class="sheet-chat-body">
        <SpaceDetailRightChat
          :key="`mobile-chat-${props.id}`"
          :space-id="props.id"
          :space="space"
          @message="handleMobileChatMessage"
        />
      </div>
    </div>

    <!-- 移动端 Apple/微信/飞书 风格 【切换空间】抽屉 (1:1 高保真复用侧边栏卡片列表与新建空间按钮) -->
    <div v-if="showMobileSpaceSheet" class="apple-overlay mobile-space-sheet-overlay" @click="showMobileSpaceSheet = false" @touchmove.self.prevent>
      <div class="apple-modal-sheet mobile-space-sheet" @click.stop @touchmove.stop>
        <div class="drag-indicator"></div>
        <div class="sheet-header">
          <h3 class="sheet-title">{{ $t('pages.spaceDetailPage.mobile.switchSpace') }}</h3>
          <button class="sheet-close-btn" @click="showMobileSpaceSheet = false">
            <yuemu-icon name="close" :size="18" />
          </button>
        </div>

        <div class="sheet-sider-body">
          <!-- 胶囊新建按钮独占顶栏，自带通透完美的上下 16px 呼吸间距 -->
          <div class="sheet-create-btn-wrap">
            <button class="pill-create-space-btn" @click="handleCreateFromSheet">
              <yuemu-icon name="plus" :size="16" class="icon-plus" />
              <span>{{ $t('pages.spaceDetailPage.overview.createSpace') }}</span>
            </button>
          </div>

          <div class="spaces-list custom-scrollbar" @scroll="handleMobileSpacesScroll">
            <div
              v-for="spaceItem in mySpaceList"
              :key="spaceItem.id"
              class="space-card-item"
              :class="{ 'is-active': String(props.id) === String(spaceItem.id) }"
              @click="handleSelectSpaceFromSheet(spaceItem)"
            >
              <!-- 空间封面/图标 -->
              <div class="space-avatar-wrap">
                <img v-if="spaceItem.spaceCover" :src="spaceItem.spaceCover" class="space-avatar-img"  alt="" />
                <div v-else class="space-avatar-fallback" :style="{ background: getFallbackGradient(spaceItem.id) }">
                  <span>{{ (spaceItem.spaceName || 'S').substring(0, 1) }}</span>
                </div>
              </div>

              <!-- 空间简讯 -->
              <div class="space-card-main">
                <div class="space-title-row">
                  <span class="space-name" :title="spaceItem.spaceName">{{ spaceItem.spaceName || $t('pages.spaceDetailPage.overview.unnamedSpace') }}</span>
                </div>
                <div class="space-sub-row">
                  <span class="meta-text">{{ $t('pages.spaceDetailPage.info.imageCount', { count: spaceItem.totalCount || spaceItem.totalSizeCount || 0 }) }}</span>
                  <div class="space-tags-row">
                    <span class="space-type-tag" :class="spaceItem.spaceType === 0 ? 'is-private' : 'is-team'">
                      {{ spaceItem.spaceType === 0 ? $t('pages.spaceDetailPage.overview.privateTag') : $t('pages.spaceDetailPage.overview.teamTag') }}
                    </span>
                    <span
                      v-if="spaceItem.spaceType !== 0 && spaceItem.visibility !== undefined && spaceItem.visibility !== null"
                      class="space-visibility-tag"
                      :class="getSpaceVisibilityClass(spaceItem.visibility)"
                    >
                      {{ getSpaceVisibilityText(spaceItem.visibility) }}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 空间列表触底加载提示 -->
            <div v-if="spacesLoadingMore" class="spaces-loading-more-tip">
              <span class="yuemu-spinner"></span>
              <span>{{ $t('pages.spaceDetailPage.status.loadingSpaces') }}</span>
            </div>
            <div v-else-if="!spacesHasMore && mySpaceList.length > 0" class="spaces-no-more-tip">
              <span>{{ $t('pages.spaceDetailPage.status.allSpacesLoaded') }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空间全景分享海报弹窗 (ShareModal) -->
    <ShareModal
      ref="shareModalRef"
      :title="space.spaceName || $t('pages.spaceDetailPage.info.spaceDetail')"
      :link="shareSpaceLink"
      :imageUrl="space.spaceCover || 'https://static.yuemutuku.com/post/1866450683272450049/2026-07-28_wgPlyGoK33wM5BZg.webp'"
      :user="space.user || loginUserStore.loginUser"
      :createTime="space.createTime"
    />

    <!-- 待开发功能轻量级 Apple 弹窗 -->
    <div v-if="showPendingModal" class="apple-overlay" @click="showPendingModal = false" style="z-index: 99999;">
      <div class="apple-modal-sheet" style="max-width: 380px; text-align: center; padding: 32px 24px; background: #ffffff; border-radius: 24px; box-shadow: 0 20px 40px rgba(0,0,0,0.15);" @click.stop>
        <div style="width: 56px; height: 56px; border-radius: 18px; background: rgba(43, 127, 255, 0.1); color: #2b7fff; display: flex; align-items: center; justify-content: center; font-size: 26px; margin: 0 auto 16px;">
          <yuemu-icon name="bolt" :size="26" />
        </div>
        <h3 style="font-size: 20px; font-weight: 700; color: #111111; margin-bottom: 8px;">{{ $t('pages.spaceDetailPage.pendingModal.title') }}</h3>
        <p style="font-size: 14px; color: #666666; margin-bottom: 24px; line-height: 1.6;">{{ $t('pages.spaceDetailPage.pendingModal.desc') }}</p>
        <button class="apple-btn-primary" style="width: 100%; border-radius: 14px; height: 42px; font-weight: 600;" @click="showPendingModal = false">{{ $t('pages.spaceDetailPage.pendingModal.confirm') }}</button>
      </div>
    </div>

    <!-- 右下角高奢纵向阶梯胶囊快捷创作菜单 (FAB Floating Pill Stack) -->
    <div
      v-if="userSpaceStatus.isMember || isPublicPreview"
      class="fab-radial-container"
      :class="{ 'is-active': fabActive }"
    >
      <!-- 背景暗光遮罩 (触碰关闭) -->
      <div class="fab-backdrop" v-if="fabActive" @click="fabActive = false"></div>

      <!-- 纵向阶梯高奢胶囊按键列表 -->
      <div class="fab-pill-stack">
        <!-- 按钮 1：上传图片 (所有成员可见) -->
        <div
          class="fab-pill-item item-picture"
          :class="{ 'is-visible': fabActive }"
          @click="handleFabAction('picture')"
        >
          <div class="fab-pill-btn btn-picture">
            <yuemu-icon name="camera" :size="16" />
            <span>{{ $t('pages.spaceDetailPage.fab.uploadPicture', '上传图片') }}</span>
          </div>
        </div>

        <!-- 按钮 2：发布动态/帖子 (所有成员可见) -->
        <div
          class="fab-pill-item item-post"
          :class="{ 'is-visible': fabActive }"
          @click="handleFabAction('post')"
        >
          <div class="fab-pill-btn btn-post">
            <yuemu-icon name="edit" :size="16" />
            <span>{{ $t('pages.spaceDetailPage.fab.publishPost', '发布动态') }}</span>
          </div>
        </div>

        <!-- 按钮 3：创建活动 (仅团队空间且有权限的创建者/管理员可见) -->
        <div
          v-if="space?.spaceType === SPACE_TYPE_ENUM.TEAM && (canManageActivity || canManageSpace || isSpaceOwner)"
          class="fab-pill-item item-activity"
          :class="{ 'is-visible': fabActive }"
          @click="handleFabAction('activity')"
        >
          <div class="fab-pill-btn btn-activity">
            <yuemu-icon name="flag" :size="16" />
            <span>{{ $t('pages.spaceDetailPage.fab.createActivity', '创建活动') }}</span>
          </div>
        </div>
      </div>

      <!-- 主加号按钮 (旋转 135° 变 × 关闭) -->
      <button
        class="fab-main-btn"
        :class="{ 'is-open': fabActive }"
        @click.stop="fabActive = !fabActive"
        :title="$t('pages.spaceDetailPage.fab.title', '快捷创作')"
      >
        <yuemu-icon name="plus" :size="22" class="fab-icon" />
      </button>
    </div>

    <!-- 活跃成员排行榜弹窗 (纯 div 原生搭建，全量支持 Dark Mode + 触底加载) -->
    <div v-if="rankModalVisible" class="apple-overlay" @click.self="rankModalVisible = false">
      <div class="yuemu-custom-modal rank-custom-modal" @click.stop>
        <!-- 弹窗 Header -->
        <div class="yuemu-custom-modal-header">
          <div class="rank-title-wrap">
            <div class="rank-icon-badge">
              <yuemu-icon name="crown" :size="18" />
            </div>
            <div class="rank-title-group">
              <h3 class="rank-title">{{ $t('pages.spaceDetailPage.activeMembers.title', '活跃成员榜') }}</h3>
              <span class="rank-sub">{{ $t('pages.spaceDetailPage.activeMembers.subtitle', '按贡献值与互动积分实时排序') }}</span>
            </div>
          </div>
          <button class="yuemu-custom-modal-close" @click="rankModalVisible = false">
            <yuemu-icon name="close" :size="18" />
          </button>
        </div>

        <!-- 成员排行榜触底加载列表区 -->
        <div class="rank-modal-scroll-area custom-scroll" @scroll="handleRankModalScroll">
          <div v-if="rankMembersLoading && rankMembersList.length === 0" class="rank-modal-loading">
            <a-spin size="large" />
          </div>

          <div v-else-if="!rankMembersLoading && rankMembersList.length === 0" class="rank-modal-empty">
            <div class="empty-icon-circle">
              <yuemu-icon name="crown" :size="22" />
            </div>
            <h4 class="empty-title">{{ $t('pages.spaceDetailPage.activeMembers.emptyTitle', '暂无成员排行数据') }}</h4>
            <p class="empty-desc">{{ $t('pages.spaceDetailPage.activeMembers.emptyDesc', '成员发布动态、上传图片与参与互动的贡献值将实时展示在这里') }}</p>
          </div>

          <div v-else class="rank-members-list">
            <div
              v-for="(m, idx) in rankMembersList"
              :key="m.userId || m.id || idx"
              class="rank-member-row"
              @click="handleUserClick(m)"
            >
              <!-- 排名 Badge (#1 #2 #3 独享金银铜徽章) -->
              <div class="rank-num-badge" :class="'rank-' + (idx + 1)">
                <yuemu-icon v-if="idx === 0" name="crown" :size="13" class="rank-crown-gold" />
                <yuemu-icon v-else-if="idx === 1" name="crown" :size="13" class="rank-crown-silver" />
                <yuemu-icon v-else-if="idx === 2" name="crown" :size="13" class="rank-crown-bronze" />
                <span v-else>{{ idx + 1 }}</span>
              </div>

              <!-- 用户头像 -->
              <img
                :src="m.user?.userAvatar || m.userAvatar || getDefaultAvatar(m.user?.userName || m.userName || $t('pages.spaceDetailPage.info.member'))"
                class="rank-member-avatar"
               alt="" />

              <!-- 用户信息 -->
              <div class="rank-member-info">
                <span class="rank-member-name">{{ m.user?.userName || m.userName || $t('pages.spaceDetailPage.info.member') }}</span>
                <span class="rank-member-role">
                  {{ String(m.userId) === String(space?.userId) ? $t('pages.spaceDetailPage.members.roleCreator') : getRoleSimpleName(m.spaceRole, false) }}
                </span>
              </div>

              <!-- 贡献值 / 积分 -->
              <div class="rank-member-score">
                <yuemu-icon name="bolt" :size="13" />
                <span>{{ m.score ?? Math.max(10, 100 - idx * 5) }} {{ $t('pages.spaceDetailPage.activeMembers.points', '贡献值') }}</span>
              </div>
            </div>
          </div>

          <!-- 触底加载 Spinner / 已加载全部提示 -->
          <div v-if="rankMembersList.length > 0" class="rank-footer-bar">
            <div v-if="rankMembersLoadingMore" class="rank-loading-more">
              <span class="yuemu-spinner"></span>
              <span>{{ $t('pages.spaceDetailPage.activeMembers.loadingMore') }}</span>
            </div>
            <div v-else-if="!rankMembersHasMore" class="rank-no-more">
              <span>{{ $t('pages.spaceDetailPage.activeMembers.noMore') }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { useI18n } from 'vue-i18n';

import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { PieChart } from 'echarts/charts';
import { TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components';

use([CanvasRenderer, PieChart, TitleComponent, TooltipComponent, LegendComponent]);
import { h, onMounted, onUnmounted, reactive, computed, watch, ref, createVNode, onActivated, onDeactivated, nextTick, provide } from 'vue'

// 在私有空间和团队空间中禁用所有子组件的广告
provide('enableAds', false)

import { getSpaceVoByIdUsingGet, getSpaceFeedUsingGet, searchSpaceContentUsingPost, updateSpaceAnnouncementUsingPost } from '@/api/spaceController.ts'
import message from 'ant-design-vue/es/message'
import Modal from 'ant-design-vue/es/modal'
import Badge from 'ant-design-vue/es/badge'
import { listPictureVoByPageUsingPost, searchPictureByColorUsingPost, searchPictureBySemanticUsingPost, searchPictureByPictureUsingPost } from '@/api/pictureController.ts'
import { formatSize } from '@/utils'
import VChart from 'vue-echarts'
import * as echarts from 'echarts'
import PictureList from '@/components/PictureList.vue'
import ShareModal from '@/components/ShareModal.vue'
import { getDeviceType } from '@/utils/device.ts'
import { DEVICE_TYPE_ENUM } from '@/constants/device.ts'
import { ColorPicker } from 'vue3-colorpicker'
import 'vue3-colorpicker/style.css'
import PictureSearchForm from '@/components/PictureSearchForm.vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { listSpaceVoByPageUsingPost } from '@/api/spaceController'
import { useRouter } from 'vue-router'

import { SPACE_PERMISSION_ENUM, SPACE_TYPE_ENUM, SPACE_VISIBILITY_ENUM } from '@/constants/space.ts'
import { prevRoute } from '@/router'
import { Pagination as VanPagination, Field as VanField, Button as VanButton, DropdownMenu as VanDropdownMenu, DropdownItem as VanDropdownItem, Loading as VanLoading, Dialog as VanDialog } from 'vant'
import 'vant/es/pagination/style'
import 'vant/es/field/style'
import 'vant/es/button/style'
import 'vant/es/dropdown-menu/style'
import 'vant/es/dropdown-item/style'
import 'vant/es/loading/style'
import 'vant/es/dialog/style'
import BigPictureList from '@/components/BigPictureList.vue'
import YuemuIcon from '@/components/icons/YuemuIcon.vue'
import { quitSpaceUsingPost, checkPendingApplicationUsingPost, joinSpaceUsingPost, listMyTeamSpaceUsingPost } from '@/api/spaceUserController'
import { listSpaceUserUsingPost } from '@/api/spaceUserController'
import UploadProgressOverlay from '@/components/upload/UploadProgressOverlay.vue'
import { useUploadProgress } from '@/composables/useUploadProgress'
import { uploadFile } from '@/utils/uploadUtil'
import { getDefaultAvatar } from '@/utils/userUtils'
import SpaceDetailLeftSider from '@/components/SpaceDetailLeftSider.vue'
import SpaceDetailRightChat from '@/components/SpaceDetailRightChat.vue'
import SpaceFeedList from '@/components/SpaceFeedList.vue'

defineOptions({
  name: 'SpaceDetail'
})

// PC 端侧边栏与聊天室展开/折叠状态控制 (持久化至 localStorage)
const showLeftSider = ref<boolean>(localStorage.getItem('yuemu_space_left_sider_open') !== 'false')
const showRightChat = ref<boolean>(localStorage.getItem('yuemu_space_right_chat_open') !== 'false')

const toggleLeftSider = () => {
  showLeftSider.value = !showLeftSider.value
  localStorage.setItem('yuemu_space_left_sider_open', String(showLeftSider.value))
}

const toggleRightChat = () => {
  showRightChat.value = !showRightChat.value
  localStorage.setItem('yuemu_space_right_chat_open', String(showRightChat.value))
}

// 移动端三栏拆分底栏状态控制
const activeMobileBarTab = ref<'spaces' | 'content' | 'chat'>('content')

const mobileBottomNavTabs = computed(() => {
  const tabs = [
    { key: 'spaces', label: t('pages.spaceDetailPage.tabs.spaces'), icon: 'layer' },
    { key: 'content', label: t('pages.spaceDetailPage.tabs.content'), icon: 'compass' }
  ]
  // 仅在真实团队空间且用户是合法成员时，才在移动端底部出现【聊天】Tab
  if (space.value?.spaceType === SPACE_TYPE_ENUM.TEAM && userSpaceStatus.value.isMember) {
    tabs.push({ key: 'chat', label: t('pages.spaceDetailPage.tabs.chat'), icon: 'chat' })
  }
  return tabs
})

const handleMobileBottomTabClick = (tabKey: string) => {
  activeMobileBarTab.value = tabKey as any
}

const shareModalRef = ref<InstanceType<typeof ShareModal>>()
const shareSpaceLink = computed(() => {
  if (typeof window !== 'undefined') return window.location.href
  return `https://yuemutuku.com/space/${props.id}`
})

const handleShareSpace = () => {
  if (shareModalRef.value) {
    shareModalRef.value.openModal()
  }
}

// 统一上传进度状态管理
const {
  progress: uploadProgress,
  phase: uploadPhase,
  errorMessage: uploadErrorMessage,
  isActive: isUploadActive,
  startUpload,
  updateProgress,
  setConfirming,
  complete: completeUpload,
  fail: failUpload,
} = useUploadProgress()

const uploadFileLabel = ref('')
import { listActivityBySpaceIdUsingPost } from '@/api/activityController'

const { t } = useI18n();

declare namespace API {
  interface SpaceVO {
    id?: string | number
    spaceName?: string
    spaceDesc?: string
    spaceCover?: string
    userId?: string | number
    user?: { userName?: string }
    spaceType?: number
    createTime?: string | number | Date
    totalSize?: number
    maxSize?: number
    totalCount?: number
    maxCount?: number
    maxStorage?: number
    usedStorage?: number
    permissionList?: string[]
  }
  interface PictureQueryRequest {
    current?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    orderBy?: string[]
    spaceId?: string | number
    [key: string]: any
  }
  interface PictureVO {
    id?: string | number
    [key: string]: any
  }
  interface SpaceEditRequest {
    id?: string | number
    spaceName?: string
    spaceDesc?: string
    spaceCover?: string
  }
}

interface Props { id: string | number }
const props = defineProps<Props>()

const loginUserStore = useLoginUserStore()
const device = ref<string>(getDeviceType())
const moreModalVisible = ref(false)
const spacePageRef = ref<HTMLElement | null>(null)
const contentWrapRef = ref<HTMLElement | null>(null)

const cameraInputRef = ref<HTMLInputElement | null>(null)
const handleCameraUpload = async (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    message.warning(t('pages.spaceDetailPage.msgs.selectImage'))
    return
  }
  
  uploadFileLabel.value = file.name
  startUpload()
  message.loading({ content: t('pages.spaceDetailPage.msgs.analyzingImage'), key: 'imageSearch', duration: 0 })
  try {
    const result = await uploadFile(file, {
      isPost: true,
      onProgress: updateProgress,
      onPhase: (p) => p === 'confirming' && setConfirming(),
    })
    completeUpload()
    message.success({ content: t('common.message.uploadSuccess'), key: 'imageSearch', duration: 2 })
    onPictureSearch(result.url)
  } catch (error: any) {
    failUpload(error?.message || t('common.message.operationFailed'))
    message.error({ content: t('common.message.operationFailed') + ': ' + error.message, key: 'imageSearch', duration: 2 })
  } finally {
    if (target) target.value = ''
    fabExpanded.value = false
  }
}



const windowWidth = ref(window.innerWidth)
const handleResize = () => { windowWidth.value = window.innerWidth }
onMounted(() => { window.addEventListener('resize', handleResize) })
onUnmounted(() => { window.removeEventListener('resize', handleResize) })
const isMobile = computed(() => windowWidth.value <= 768)

const showSearchPopup = ref(false)
const showColorPickerInPopup = ref(false)
type SpaceSearchType = 'all' | 'picture' | 'post' | 'activity'
const spaceSearchKeyword = ref('')
const spaceSearchType = ref<SpaceSearchType>('all')
const spaceSearchResults = ref<API.SpaceFeedItemVO[]>([])
const spaceSearchPage = ref(1)
const spaceSearchTotal = ref(0)
const spaceSearchLoading = ref(false)
const spaceSearchHasRun = ref(false)
const spaceSearchTabs = computed(() => [
  { key: 'all' as SpaceSearchType, label: t('pages.spaceDetailPage.tabs.all', '全部') },
  { key: 'picture' as SpaceSearchType, label: t('pages.spaceDetailPage.tabs.pictures', '图片') },
  { key: 'post' as SpaceSearchType, label: t('pages.spaceDetailPage.tabs.post', '动态') },
  { key: 'activity' as SpaceSearchType, label: t('pages.spaceDetailPage.tabs.activity', '活动') },
])
const spaceSearchHasMore = computed(() => spaceSearchResults.value.length < spaceSearchTotal.value)

const handleOpenSearchPopup = (e?: Event) => {
  if (e) e.stopPropagation()
  showSearchPopup.value = true
}

const handleCloseSearchPopup = (e?: Event) => {
  if (e) e.stopPropagation()
  showSearchPopup.value = false
}

const doPopupSearch = () => {
  searchSpaceContent(true)
}

const selectSpaceSearchType = (type: SpaceSearchType) => {
  spaceSearchType.value = type
  if (spaceSearchHasRun.value && spaceSearchKeyword.value.trim()) searchSpaceContent(true)
}

const searchSpaceContent = async (reset: boolean) => {
  const keyword = spaceSearchKeyword.value.trim()
  if (!keyword || !props.id) return
  if (spaceSearchLoading.value) return
  spaceSearchLoading.value = true
  if (reset) spaceSearchPage.value = 1
  try {
    const res = await searchSpaceContentUsingPost({
      spaceId: String(props.id) as any,
      keyword,
      type: spaceSearchType.value,
      current: spaceSearchPage.value,
      pageSize: 20,
    })
    if (res.data.code === 0 && res.data.data) {
      const records = res.data.data.records || []
      spaceSearchResults.value = reset ? records : [...spaceSearchResults.value, ...records]
      spaceSearchTotal.value = Number(res.data.data.total || 0)
      spaceSearchHasRun.value = true
      if (records.length) spaceSearchPage.value += 1
    }
  } catch {
    message.error(t('pages.spaceDetailPage.msgs.spaceSearchFail'))
  } finally {
    spaceSearchLoading.value = false
  }
}

const getSearchResultImage = (item: API.SpaceFeedItemVO) => item.type === 'picture'
  ? (item.thumbnailUrl || item.url || item.coverUrl) : item.coverUrl
const openSearchResult = (item: API.SpaceFeedItemVO) => {
  if (item.type === 'picture') router.push({ name: 'PictureDetail', params: { id: String(item.id) } })
  else openSpaceFeedItem(item)
  showSearchPopup.value = false
}
const applyPictureSearchResults = (pictures: API.PictureVO[]) => {
  spaceSearchResults.value = pictures.map((picture: any) => ({
    ...picture, type: 'picture', title: picture.name, thumbnailUrl: picture.thumbnailUrl || picture.url,
  }))
  spaceSearchTotal.value = spaceSearchResults.value.length
  spaceSearchPage.value = 1
  spaceSearchHasRun.value = true
}
const runSemanticSearch = async () => {
  const keyword = spaceSearchKeyword.value.trim()
  if (!keyword || !props.id) {
    message.warning(t('pages.spaceDetailPage.msgs.semanticDescriptionRequired'))
    return
  }
  spaceSearchLoading.value = true
  try {
    const res = await searchPictureBySemanticUsingPost({ searchText: keyword, spaceId: String(props.id) as any, current: 1, pageSize: 20 })
    if (res.data.code === 0) applyPictureSearchResults(res.data.data?.records || [])
  } finally {
    spaceSearchLoading.value = false
  }
}
const PRESET_COLOR_KEYS = ['chineseRed','vividOrange','lemonYellow','emeraldGreen','mintCyan','oceanBlue','violet','sakuraPink','pureWhite','carbonBlack','earthBrown','slateGray'] as const
const PRESET_COLOR_VALUES = ['#EF4444','#F97316','#EAB308','#10B981','#06B6D4','#3B82F6','#8B5CF6','#EC4899','#FFFFFF','#1E293B','#78350F','#64748B']
const presetColors = computed(() => PRESET_COLOR_KEYS.map((key, i) => ({
  name: t(`pages.spaceDetailPage.presetColorNames.${key}`),
  value: PRESET_COLOR_VALUES[i]
})))

const selectedHexColor = ref('#3B82F6')

const openColorPicker = () => {
  showColorPickerInPopup.value = !showColorPickerInPopup.value
}

const selectPresetColor = (hex: string) => {
  selectedHexColor.value = hex
  onColorChange(hex)
  showColorPickerInPopup.value = false
}

const confirmCustomColorSearch = () => {
  if (!selectedHexColor.value) return
  onColorChange(selectedHexColor.value)
  showColorPickerInPopup.value = false
}

const fabExpanded = ref(false)



const toggleFab = () => {
  fabExpanded.value = !fabExpanded.value
}







const isStatusLoading = ref(true)

const toggleMoreModal = () => {
  moreModalVisible.value = !moreModalVisible.value
  document.body.style.overflow = moreModalVisible.value ? 'hidden' : ''
}

onMounted(() => {
  device.value = getDeviceType()
  if (device.value !== DEVICE_TYPE_ENUM.PC && !props.id) {
    showMobileSpaceSheet.value = true
  }
})

const savedScrollPosition = ref(0)
const scrollPositionFrozen = ref(false)

const saveScrollPosition = () => {
  savedScrollPosition.value = spacePageRef.value?.scrollTop || 0
}

const restoreScrollPosition = () => {
  nextTick(() => {
    if (spacePageRef.value) {
      spacePageRef.value.scrollTop = savedScrollPosition.value
    }
  })
}

onActivated(() => {
  bindMainScrollListener()
  restoreScrollPosition()
  nextTick(() => {
    scrollPositionFrozen.value = false
  })
})

onDeactivated(() => {
  if (!scrollPositionFrozen.value) saveScrollPosition()
  scrollPositionFrozen.value = true
  unbindMainScrollListener()
})

// 移动端 2 态聊天 Handle 抽屉状态 ('collapsed' 52px | 'expanded' 85vh)
const mobileChatSheetState = ref<'collapsed' | 'expanded'>('collapsed')
const latestChatMessageInfo = reactive({
  sender: '',
  content: ''
})

const handleMobileChatMessage = (msg: any) => {
  if (!msg) return
  const senderName = msg.user?.userName || msg.userName || t('pages.spaceDetailPage.info.member')
  const textContent = msg.content || msg.text || msg.message || ''
  if (textContent) {
    latestChatMessageInfo.sender = senderName
    latestChatMessageInfo.content = textContent
  }
}

const toggleMobileChatSheetState = () => {
  mobileChatSheetState.value = mobileChatSheetState.value === 'collapsed' ? 'expanded' : 'collapsed'
}

let touchStartY = 0
let touchMoveY = 0

const handleChatSheetTouchStart = (e: TouchEvent) => {
  touchStartY = e.touches[0].clientY
  touchMoveY = touchStartY
}

const handleChatSheetTouchMove = (e: TouchEvent) => {
  touchMoveY = e.touches[0].clientY
}

const handleChatSheetTouchEnd = () => {
  const deltaY = touchMoveY - touchStartY
  if (deltaY < -30) {
    mobileChatSheetState.value = 'expanded'
  } else if (deltaY > 30) {
    mobileChatSheetState.value = 'collapsed'
  }
}

const router = useRouter()
const handleNavigate = (target: { name?: string; params?: any; query?: any }) => {
  router.push(target)
}
const space = ref<API.SpaceVO>({})
const searchFormRef = ref()

// ── 切换空间时重置所有状态，防止脏数据污染 ──
const resetSpaceState = () => {
  space.value = {}
  dataList.value = []
  total.value = 0
  hasMore.value = true
  loading.value = true
  isStatusLoading.value = true
  activeNavTab.value = 'dynamics'
  activities.value = []
  activitiesLoadingMore.value = false
  activitiesHasMore.value = true
  activityParams.current = 1
  spaceMemberList.value = []
  paginatedMembers.value = []
  membersHasMore.value = true
  memberParams.current = 1
  Object.assign(searchParams, { current: 1, pageSize: 20, searchText: undefined, sortField: 'createTime', sortOrder: 'descend' })
}



// 侧边栏全量空间列表数据与分页触底加载管理
const mySpaceList = ref<API.SpaceVO[]>([])
const spacesSearchParams = reactive({
  current: 1,
  pageSize: 10
})
const spacesHasMore = ref(true)
const spacesLoadingMore = ref(false)

const fetchMySpacesList = async () => {
  try {
    spacesSearchParams.current = 1
    spacesHasMore.value = false

    const teamRes = await listMyTeamSpaceUsingPost({})
    if (teamRes.data?.code === 0 && teamRes.data.data) {
      mySpaceList.value = teamRes.data.data || []
    } else {
      mySpaceList.value = []
    }
  } catch (error) {
    console.error('获取所有关联空间失败:', error)
    mySpaceList.value = []
  }
}

const fetchMoreSpaces = async () => {
  if (spacesLoadingMore.value || !spacesHasMore.value) return
  spacesLoadingMore.value = true
  try {
    const nextPage = spacesSearchParams.current + 1
    const res = await listSpaceVoByPageUsingPost({
      current: nextPage,
      pageSize: spacesSearchParams.pageSize,
      userId: loginUserStore.loginUser?.id
    })
    if (res.data?.code === 0 && res.data.data?.records) {
      const newRecords = res.data.data.records
      if (newRecords.length > 0) {
        const map = new Map<string | number, API.SpaceVO>()
        mySpaceList.value.forEach(s => s.id && map.set(s.id, s))
        newRecords.forEach((s: API.SpaceVO) => s.id && map.set(s.id, s))
        mySpaceList.value = Array.from(map.values())
        spacesSearchParams.current = nextPage
      }
      spacesHasMore.value = (res.data.data?.records?.length ?? 0) === spacesSearchParams.pageSize
    } else {
      spacesHasMore.value = false
    }
  } catch (error) {
    console.error('加载更多空间失败:', error)
  } finally {
    spacesLoadingMore.value = false
  }
}

const handleMobileSpacesScroll = (e: Event) => {
  const target = e.target as HTMLElement
  if (!target) return
  if (target.scrollHeight - target.scrollTop - target.clientHeight < 30) {
    fetchMoreSpaces()
  }
}

const activeNavTab = ref('dynamics')
const spacePosts = ref<API.SpaceFeedItemVO[]>([])
const spacePostsTotal = ref(0)
const spacePostsLoading = ref(false)
const spacePostsLoadingMore = ref(false)
const spacePostsHasMore = ref(true)
const spacePostPage = ref(1)
const spacePostPageSize = 10

const fetchSpacePosts = async (loadMore = false) => {
  if (!props.id || spacePostsLoading.value || spacePostsLoadingMore.value) return
  if (loadMore) spacePostsLoadingMore.value = true
  else {
    spacePostsLoading.value = true
    spacePostPage.value = 1
  }
  try {
    const res = await getSpaceFeedUsingGet({
      // Snowflake IDs exceed JavaScript's safe integer range; keep them as strings.
      spaceId: String(props.id) as any,
      current: spacePostPage.value,
      pageSize: spacePostPageSize,
    })
    if (res.data.code === 0 && res.data.data) {
      const records = res.data.data.records || []
      spacePosts.value = loadMore ? [...spacePosts.value, ...records] : records
      spacePostsTotal.value = Number(res.data.data.total || 0)
      spacePostsHasMore.value = spacePosts.value.length < spacePostsTotal.value
    }
  } catch (error) {
    console.error('获取空间动态失败:', error)
    message.error(t('pages.spaceDetailPage.feed.loadFailed'))
  } finally {
    spacePostsLoading.value = false
    spacePostsLoadingMore.value = false
  }
}

const loadMoreSpacePosts = async () => {
  if (!spacePostsHasMore.value || spacePostsLoadingMore.value) return
  spacePostPage.value += 1
  await fetchSpacePosts(true)
}

const openSpacePostEditor = () => router.push({ name: 'PostEdit', query: { spaceId: String(props.id) } })
const openSpaceFeedItem = (item: any) => {
  if (!item.id) return
  if (item.type === 'picture') return // SpaceFeedList 内部已弹窗处理，无需重复跳转
  else if (item.type === 'activity') handleActivityClick(item.id)
  else router.push({ name: 'PostDetail', params: { id: item.id } })
}
const getSpaceFeedImage = (item: any) => item.type === 'picture' ? (item.thumbnailUrl || item.url) : item.coverUrl
const getSpaceFeedTypeText = (type: string) => {
  if (type === 'picture') return t('pages.spaceDetailPage.tabs.pictures', '图片')
  if (type === 'activity') return t('pages.spaceDetailPage.tabs.activity', '活动')
  return t('pages.spaceDetailPage.tabs.post', '动态')
}
const getSpaceFeedTypeIcon = (type?: string) => {
  if (type === 'picture') return 'camera'
  if (type === 'activity') return 'clock'
  return 'edit'
}
const stripPostContent = (content: string) => content.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()

watch(
  [() => props.id, () => loginUserStore.loginUser?.id],
  async ([newSpaceId], oldValues) => {
    const oldSpaceId = oldValues?.[0]
    if (String(newSpaceId || '') !== String(oldSpaceId || '')) {
      activeNavTab.value = 'dynamics'
      spacePosts.value = []
      spacePostsTotal.value = 0
      spacePostsHasMore.value = true
      activeMobileBarTab.value = 'content'
    }
    if (mySpaceList.value.length === 0) {
      await fetchMySpacesList()
    }
  },
  { immediate: true }
)

watch(
  () => activeNavTab.value,
  (newTab) => {
    if (newTab === 'dynamics' && props.id && spacePosts.value.length === 0 && !spacePostsLoading.value) {
      fetchSpacePosts()
    }
  }
)



const handleSelectSpaceFromSider = (selectedSpace: API.SpaceVO) => {
  if (!selectedSpace.id) return
  if (String(selectedSpace.id) !== String(props.id)) {
    saveScrollPosition()
    scrollPositionFrozen.value = true
    router.push({ name: 'SpaceDetail', params: { id: selectedSpace.id } })
  }
}

const handleCreateSpaceFromSider = () => {
  router.push('/add_space')
}

const handleOpenSettingsFromSider = () => {
  router.push('/setting')
}

const handleSelectNavFromSider = (navKey: string) => {
  if (navKey === 'home') router.push('/')
  else if (navKey === 'discover') router.push('/search')
  else if (navKey === 'favorite') router.push('/favorite')
}

// PC 端中间 Banner 所需的空间展示名称计算属性
const displaySpaceTitle = computed(() => {
  const name = space.value?.spaceName
  if (name && typeof name === 'string' && name.trim() !== '') {
    return name
  }
  if (space.value?.user?.userName) {
    return t('pages.spaceDetailPage.info.userSpace', { name: space.value.user.userName })
  }
  return t('pages.spaceDetailPage.info.spaceDetail')
})

// 真实空间成员列表拉取（含去重防刷保护）
const spaceMemberList = ref<any[]>([])
let isFetchingMembers = false

const fetchSpaceMembers = async () => {
  if (!props.id || isFetchingMembers) return
  isFetchingMembers = true
  try {
    const res = await listSpaceUserUsingPost({ spaceId: props.id.toString(), status: 1 })
    if (res.data?.code === 0 && res.data.data) {
      spaceMemberList.value = res.data.data?.records || res.data.data || []
    }
  } catch (e) {
    console.error('获取空间成员列表失败:', e)
  } finally {
    isFetchingMembers = false
  }
}

const activeMembers = computed(() => (space.value as any)?.activeMembers || [])

const rankModalVisible = ref(false)
const rankMembersList = ref<any[]>([])
const rankMembersLoading = ref(false)
const rankMembersLoadingMore = ref(false)
const rankMembersHasMore = ref(true)
const rankCurrentPage = ref(1)
const rankPageSize = 10

const openRankModal = () => {
  rankModalVisible.value = true
  if (rankMembersList.value.length === 0) {
    fetchRankMembers()
  }
}

const fetchRankMembers = async (isLoadMore = false) => {
  if (!props.id) return
  if (isLoadMore) {
    if (rankMembersLoadingMore.value || !rankMembersHasMore.value) return
    rankMembersLoadingMore.value = true
  } else {
    rankMembersLoading.value = true
    rankCurrentPage.value = 1
    rankMembersHasMore.value = true
  }

  try {
    const res = await listSpaceUserUsingPost({
      spaceId: props.id.toString(),
      status: 1,
      current: rankCurrentPage.value,
      pageSize: rankPageSize
    } as any)

    if (res.data?.code === 0 && res.data?.data) {
      const records = res.data.data?.records || (Array.isArray(res.data.data) ? res.data.data : [])
      const total = Number(res.data.data?.total || records.length) || 0

      // 合并 activeMembers 贡献值并降序排序
      const activeMap = new Map((space.value as any)?.activeMembers?.map((a: any) => [String(a.userId || a.id), a.score]))
      records.forEach((item: any, idx: number) => {
        const uid = String(item.userId || item.id)
        if (item.score === undefined) {
          item.score = activeMap.get(uid) ?? Math.max(10, 100 - (rankCurrentPage.value - 1) * 10 - idx * 5)
        }
      })
      records.sort((a: any, b: any) => (b.score || 0) - (a.score || 0))

      if (isLoadMore) {
        rankMembersList.value = [...rankMembersList.value, ...records]
      } else {
        rankMembersList.value = records
      }

      rankMembersHasMore.value = rankMembersList.value.length < total && records.length >= rankPageSize
    }
  } catch (e) {
    console.error('获取活跃成员排行榜失败:', e)
  } finally {
    rankMembersLoading.value = false
    rankMembersLoadingMore.value = false
  }
}

const loadMoreRankMembers = () => {
  if (!rankMembersHasMore.value || rankMembersLoadingMore.value) return
  rankCurrentPage.value += 1
  fetchRankMembers(true)
}

const handleRankModalScroll = (e: Event) => {
  const target = e.target as HTMLElement
  if (!target) return
  const scrollTop = target.scrollTop
  const scrollHeight = target.scrollHeight
  const clientHeight = target.clientHeight
  if (scrollHeight - scrollTop - clientHeight < 40) {
    loadMoreRankMembers()
  }
}

const topMembers = computed(() => {
  const list = activeMembers.value.length > 0
    ? activeMembers.value
    : (spaceMemberList.value.length > 0
      ? spaceMemberList.value
      : ((space.value as any)?.recommendedUsers || (space.value as any)?.spaceUserList || []))
  return list.slice(0, 3).map((item: any) => ({
    id: item.userId || item.id,
    name: item.user?.userName || item.userName || t('pages.spaceDetailPage.info.member'),
    avatar: item.user?.userAvatar || item.userAvatar || getDefaultAvatar(item.user?.userName || item.userName || t('pages.spaceDetailPage.info.member'))
  }))
})

const realMemberTotal = ref<number | null>(null)

const memberTotalCount = computed(() => {
  if (realMemberTotal.value !== null && realMemberTotal.value > 0) return realMemberTotal.value
  if (paginatedMembers.value.length > 0) return paginatedMembers.value.length
  if (spaceMemberList.value.length > 0) return spaceMemberList.value.length
  const list = (space.value as any)?.recommendedUsers || (space.value as any)?.spaceUserList || []
  return space.value?.memberCount || list.length || 1
})

// 只有空间创建者 (Owner) 或管理者 (Admin) 才有权限显示【设置】与【成员管理】
const isSpaceOwner = computed(() => {
  if (!space.value?.id || !loginUserStore.loginUser?.id) return false
  return String(space.value.userId) === String(loginUserStore.loginUser.id)
})

const canManageSpace = computed(() => {
  if (!space.value?.id || !loginUserStore.loginUser?.id) return false
  // 系统管理员拥有空间管理权限
  if (loginUserStore.loginUser.userRole === 'admin') return true
  const isSpaceAdmin = (space.value.permissionList || []).includes(SPACE_PERMISSION_ENUM.SPACE_USER_MANAGE) || (space.value.permissionList || []).includes('spaceUser:manage')
  return isSpaceOwner.value || isSpaceAdmin
})

// ============ 空间公告 ============
const announcementModalOpen = ref(false)
const announcementSaving = ref(false)
const announcementDraft = ref('')

const openAnnouncementEditor = () => {
  announcementDraft.value = space.value.announcement || ''
  announcementModalOpen.value = true
}

const closeAnnouncementEditor = () => {
  announcementModalOpen.value = false
}

const saveAnnouncement = async () => {
  if (!props.id) return
  announcementSaving.value = true
  try {
    const res = await updateSpaceAnnouncementUsingPost({
      spaceId: props.id,
      announcement: announcementDraft.value.trim()
    })
    if (res.data.code === 0) {
      message.success(t('pages.spaceDetailPage.announcement.saved'))
      announcementModalOpen.value = false
      // 重新拉取空间详情，以服务端返回的公告与发布时间为准（避免本地时区/格式偏差）
      try {
        const detailRes = await getSpaceVoByIdUsingGet({ id: props.id })
        if (detailRes.data.code === 0 && detailRes.data.data) {
          space.value = detailRes.data.data
        }
      } catch (e) {
        // 刷新失败不阻塞，下次进入页面会自动同步
      }
    } else {
      message.error(t('pages.spaceDetailPage.announcement.saveFailed') + (res.data.message || ''))
    }
  } catch (error: any) {
    message.error(t('pages.spaceDetailPage.announcement.saveFailed') + (error?.message || ''))
  } finally {
    announcementSaving.value = false
  }
}
// ============ 空间公告结束 ============

// PUBLIC 团队空间的非成员预览模式
const isPublicPreview = computed(() => {
  if (!props.id || !space.value?.id) return false
  return space.value.spaceType === SPACE_TYPE_ENUM.TEAM
    && space.value.visibility === SPACE_VISIBILITY_ENUM.PUBLIC
    && !userSpaceStatus.value.isMember
})

const getSpaceVisibilityText = (visibility?: number) => {
  if (visibility === 0) return t('constants.space.visibilityPublic')
  if (visibility === 2) return t('constants.space.visibilityHidden')
  return t('constants.space.visibilityRestricted')
}

const getSpaceVisibilityClass = (visibility?: number) => {
  if (visibility === 0) return 'is-public'
  if (visibility === 2) return 'is-hidden'
  return 'is-restricted'
}

// Sub-Nav 选项卡定义：包含【动态】标签；仅创建者/管理员展示【设置】
const spaceNavTabs = computed(() => {
  const tabs = [
    { key: 'dynamics', label: t('pages.spaceDetailPage.tabs.dynamics', '动态'), icon: 'category' },
  ]
  if (space.value?.spaceType === SPACE_TYPE_ENUM.TEAM) {
    tabs.push({ key: 'members', label: t('pages.spaceDetailPage.tabs.members', '成员'), icon: 'users' })
  }
  // 仅空间创建者/管理员展示【设置】Tab
  if (canManageSpace.value || isSpaceOwner.value) {
    tabs.push({ key: 'settings', label: t('pages.spaceDetailPage.tabs.settings', '设置'), icon: 'gear' })
  }
  return tabs
})

const showPendingModal = ref(false)
const handleShowDynamicsPending = () => {
  showPendingModal.value = true
}

const handleShareClick = () => {
  message.info(t('common.message.featureUnderDev'))
}

const handleGoToAnalyze = () => {
  if (space.value?.id) {
    router.push({
      path: '/space_analyze',
      query: {
        spaceId: space.value.id,
        spaceName: space.value.spaceName,
        spaceType: space.value.spaceType,
        visibility: space.value.visibility,
      },
    })
  } else {
    router.push({ path: '/space_analyze' })
  }
}

// 成员分页加载
const paginatedMembers = ref<any[]>([])
const membersLoading = ref(false)
const membersLoadingMore = ref(false)
const membersHasMore = ref(true)

const memberParams = reactive({
  current: 1,
  pageSize: 12
})

const fetchMemberList = async (isLoadMore = false) => {
  if (!props.id) return
  if (isLoadMore) {
    if (membersLoadingMore.value || !membersHasMore.value) return
    membersLoadingMore.value = true
  } else {
    membersLoading.value = true
    memberParams.current = 1
    membersHasMore.value = true
  }

  try {
    const res = await listSpaceUserUsingPost({
      spaceId: props.id.toString(),
      current: memberParams.current,
      pageSize: memberParams.pageSize,
      status: 1
    })
    if (res.data?.code === 0 && res.data?.data) {
      const records = res.data.data.records || res.data.data || []
      const total = Number(res.data.data.total) || records.length
      realMemberTotal.value = total
      
      if (isLoadMore) {
        paginatedMembers.value = [...paginatedMembers.value, ...records]
      } else {
        paginatedMembers.value = records
      }

      membersHasMore.value = paginatedMembers.value.length < total && records.length >= memberParams.pageSize
    }
  } catch (error) {
    console.error('获取成员分页列表失败:', error)
  } finally {
    membersLoading.value = false
    membersLoadingMore.value = false
  }
}

const loadMoreMembers = async () => {
  if (!membersHasMore.value || membersLoadingMore.value || membersLoading.value) return
  memberParams.current++
  await fetchMemberList(true)
}

const getRoleSimpleName = (role?: string, isCreator = false) => {
  if (isCreator) return ''
  if (role === 'admin') return t('pages.spaceDetailPage.members.roleAdmin')
  if (role === 'editor') return t('pages.spaceDetailPage.members.roleEditor')
  return t('pages.spaceDetailPage.members.roleMember')
}

const handleUserClick = (item: any) => {
  const uid = item?.userId || item?.user?.id || item?.id
  if (uid) {
    router.push(`/user/${uid}`)
  }
}

const handleManageMembers = () => {
  if (space.value?.id) {
    router.push({ name: 'SpaceUserManage', params: { id: space.value.id } })
  }
}

const handleManageActivities = () => {
  if (space.value?.id) {
    router.push(`/space/${space.value.id}/activityManage`)
  }
}

const handleManageReview = () => {
  if (space.value?.id) {
    router.push({ name: 'SpaceReview', params: { id: String(space.value.id) } })
  }
}

const getRoleName = (role?: string) => {
  if (role === 'admin') return t('pages.spaceDetailPage.members.roleNameAdmin')
  if (role === 'editor') return t('pages.spaceDetailPage.members.roleNameEditor')
  return t('pages.spaceDetailPage.members.roleNameViewer')
}

const getRoleBadgeClass = (role?: string) => {
  if (role === 'admin') return 'is-role-admin'
  if (role === 'editor') return 'is-role-editor'
  return 'is-role-viewer'
}

const getRoleIconClass = (role?: string) => {
  if (role === 'admin') return 'crown'
  if (role === 'editor') return 'edit'
  return 'eye'
}

const activities = ref<any[]>([])
const activitiesLoading = ref(false)
const activitiesLoadingMore = ref(false)
const activitiesHasMore = ref(true)

const activityParams = reactive({
  current: 1,
  pageSize: 10
})

const fetchActivities = async (isLoadMore = false) => {
  if (!props.id) return
  if (isLoadMore) {
    if (activitiesLoadingMore.value || !activitiesHasMore.value) return
    activitiesLoadingMore.value = true
  } else {
    activitiesLoading.value = true
    activityParams.current = 1
    activitiesHasMore.value = true
  }

  try {
    const res = await listActivityBySpaceIdUsingPost({
      spaceId: props.id,
      current: activityParams.current,
      pageSize: activityParams.pageSize
    })
    if (res.data?.code === 0 && res.data?.data) {
      const records = res.data.data.records || []
      const total = Number(res.data.data.total) || 0
      
      if (isLoadMore) {
        activities.value = [...activities.value, ...records]
      } else {
        activities.value = records
      }

      activitiesHasMore.value = activities.value.length < total && records.length >= activityParams.pageSize
    }
  } catch (error) {
    console.error('获取活动列表失败:', error)
  } finally {
    activitiesLoading.value = false
    activitiesLoadingMore.value = false
  }
}

const loadMoreActivities = async () => {
  if (!activitiesHasMore.value || activitiesLoadingMore.value || activitiesLoading.value) return
  activityParams.current++
  await fetchActivities(true)
}

const handleActivityClick = (activityId: string | number) => {
  router.push({ name: 'ActivityDetail', params: { id: activityId } })
}

const formatTime = (time: string | number | Date) => {
  if (!time) return ''
  const date = new Date(time)
  if (isNaN(date.getTime())) return String(time)
  const year = String(date.getFullYear()).slice(-2)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const handleUploadClick = () => {
  router.push({ name: 'AddPicture', query: { spaceId: props.id } })
}

const fabActive = ref(false)

const handleFabAction = (actionType: 'picture' | 'post' | 'activity') => {
  fabActive.value = false
  if (actionType === 'picture') {
    handleUploadClick()
  } else if (actionType === 'post') {
    router.push({ name: 'PostEdit', query: { spaceId: props.id } })
  } else if (actionType === 'activity') {
    if (canManageActivity.value) {
      router.push({ name: 'SpaceActivityManage', query: { spaceId: props.id } })
    } else {
      message.info(t('pages.spaceDetailPage.msgs.publicActivityHint'))
    }
  }
}

function createPermissionChecker(permission: string) {
  return computed(() => (space.value.permissionList ?? []).includes(permission))
}

const canManageSpaceUser = createPermissionChecker(SPACE_PERMISSION_ENUM.SPACE_USER_MANAGE)
const canUploadPicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_UPLOAD)
const canEditPicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_EDIT)
const canDeletePicture = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_DELETE)
const canManageActivity = createPermissionChecker(SPACE_PERMISSION_ENUM.ACTIVITY_MANAGE)
const canPublishSpacePost = createPermissionChecker(SPACE_PERMISSION_ENUM.POST_PUBLISH)

// JS 原生三角函数 (Math.sin / Math.cos) 极坐标 Engine
const fabItems = computed(() => {
  const list = [
    { key: 'picture', icon: 'camera', label: t('pages.spaceDetailPage.fab.uploadPicture', '上传图片'), btnClass: 'btn-picture' },
    { key: 'post', icon: 'edit', label: t('pages.spaceDetailPage.fab.publishPost', '发布动态'), btnClass: 'btn-post' }
  ]
  if (space.value?.spaceType === SPACE_TYPE_ENUM.TEAM && (canManageActivity.value || canManageSpace.value || isSpaceOwner.value)) {
    list.push({ key: 'activity', icon: 'flag', label: t('pages.spaceDetailPage.fab.createActivity', '创建活动'), btnClass: 'btn-activity' })
  }

  const radius = 120
  const total = list.length

  return list.map((item, index) => {
    let angle = 0
    if (total === 3) {
      angle = index * 45 // 0°, 45°, 90°
    } else if (total === 2) {
      angle = index * 45 // 0°, 45°
    }

    const rad = (angle * Math.PI) / 180
    const x = Math.round(-radius * Math.sin(rad))
    const y = Math.round(-radius * Math.cos(rad))

    return {
      ...item,
      x,
      y,
      style: {
        transform: fabActive.value
          ? `translate(${x}px, ${y}px) scale(1)`
          : `translate(0px, 0px) scale(0.2)`,
        opacity: fabActive.value ? 1 : 0,
        transitionDelay: `${index * 0.05}s`
      }
    }
  })
})

const canQuitSpace = computed(() => {
  return space.value.spaceType === SPACE_TYPE_ENUM.TEAM && space.value.userId !== loginUserStore.loginUser.id
})

const openEditSpacePage = () => {
  if (space.value?.id) {
    router.push({ path: '/add_space', query: { id: String(space.value.id), type: String(space.value.spaceType) } })
  }
}

const handleQuitSpace = async () => {
  try {
    if (device.value === DEVICE_TYPE_ENUM.PC) {
      Modal.confirm({
        title: t('pages.spaceDetailPage.quitDialog.title'),
        icon: createVNode(YuemuIcon, { name: 'warning', size: 22 }),
        content: t('pages.spaceDetailPage.quitDialog.desc'),
        okText: t('pages.spaceDetailPage.quitDialog.confirm'),
        cancelText: t('pages.spaceDetailPage.quitDialog.cancel'),
        okButtonProps: { danger: true },
        async onOk() {
          try {
            const res = await quitSpaceUsingPost({ id: props.id })
            if (res.data.code === 0) {
              message.success(t('pages.spaceDetailPage.msgs.quitSuccess'))
              await router.push({ name: 'Home' })
            } else message.error(t('pages.spaceDetailPage.msgs.quitFailPrefix') + res.data.message)
          } catch (error: any) { message.error(t('pages.spaceDetailPage.msgs.quitFailPrefix') + error.message) }
        }
      })
    } else {
      showQuitConfirmDialog.value = true
    }
  } catch (error: any) {
    message.error(t('pages.spaceDetailPage.msgs.opFailPrefix') + error.message)
  }
}

const showQuitConfirmDialog = ref(false)

const confirmQuit = async () => {
  try {
    const res = await quitSpaceUsingPost({ id: props.id })
    if (res.data.code === 0) {
      message.success(t('pages.spaceDetailPage.msgs.quitSuccess'))
      showQuitConfirmDialog.value = false
      await router.push({ name: 'Home' })
    } else message.error(t('pages.spaceDetailPage.msgs.quitFailPrefix') + res.data.message)
  } catch (error: any) {
    message.error(t('pages.spaceDetailPage.msgs.quitFailPrefix') + error.message)
  }
}

const cancelQuit = () => { showQuitConfirmDialog.value = false }

const pendingApplicationsCount = ref<number>(0)
let isFetchingPending = false

const fetchPendingApplications = async () => {
  if (!props.id || isFetchingPending) return
  isFetchingPending = true
  try {
    const res = await listSpaceUserUsingPost({ spaceId: props.id.toString(), status: 0 })
    if (res.data?.code === 0) pendingApplicationsCount.value = res.data.data?.total || res.data.data?.records?.length || 0
  } catch (error) {
  } finally {
    isFetchingPending = false
  }
}

const showMobileSpaceSheet = ref(false)

watch(showMobileSpaceSheet, (val) => {
  if (val) {
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
  }
})

const getFallbackGradient = (id?: number | string) => {
  const gradients = [
    'linear-gradient(135deg, #6366f1 0%, #a855f7 100%)',
    'linear-gradient(135deg, #3b82f6 0%, #22c55e 100%)',
    'linear-gradient(135deg, #f59e0b 0%, #ef4444 100%)',
    'linear-gradient(135deg, #ec4899 0%, #8b5cf6 100%)'
  ]
  const numId = Number(id) || 0
  return gradients[numId % gradients.length]
}

const handleSelectSpaceFromSheet = (sp: API.SpaceVO) => {
  showMobileSpaceSheet.value = false
  handleSelectSpaceFromSider(sp)
}

const handleCreateFromSheet = () => {
  showMobileSpaceSheet.value = false
  handleCreateSpaceFromSider()
}

const handleTabClick = (key: string) => {
  if (!userSpaceStatus.value.isMember && !isPublicPreview.value) {
    message.warning(t('pages.spaceDetailPage.actions.notMemberWarning'))
    return
  }
  activeNavTab.value = key
  if (key === 'dynamics') fetchSpacePosts()
}

const checkPendingApplication = async () => {
  if (!props.id) return
  try {
    if (!loginUserStore.loginUser?.id) {
      message.error(t('pages.spaceDetailPage.msgs.notLoggedIn'))
      router.push({ name: 'UserLogin' })
      return
    }
    const res = await checkPendingApplicationUsingPost({ userId: loginUserStore.loginUser.id, spaceId: props.id })
    if (res.data.code === 0) {
      if (res.data.data) {
        message.info(t('pages.spaceDetailPage.msgs.applySubmitted'))
      } else {
        showJoinSpaceDialog.value = true
      }
    } else message.error(t('pages.spaceDetailPage.msgs.checkApplyFailPrefix') + res.data.message)
  } catch (error) { message.error(t('pages.spaceDetailPage.msgs.checkApplyFail')) }
}

const getAvatarUrl = (name?: string) => {
  try {
    if (typeof getDefaultAvatar === 'function') {
      return getDefaultAvatar(name || t('pages.spaceDetailPage.info.member'))
    }
  } catch (e) {}
  return 'https://static.yuemutuku.com/post/1866450683272450049/2026-07-28_9NO0axdKGmcO'
}

const spaceOverviewViewMode = ref<'grid' | 'list'>('grid')

const showJoinSpaceDialog = ref(false)

const joinSpace = async () => {
  try {
    const res = await joinSpaceUsingPost({ spaceId: props.id })
    if (res.data.code === 0) {
      message.success(t('pages.spaceDetailPage.msgs.applySuccess'))
      showJoinSpaceDialog.value = false
      await updateUserSpaceStatus()
    } else message.error(t('pages.spaceDetailPage.msgs.applyFailPrefix') + res.data.message)
  } catch (error) { message.error(t('pages.spaceDetailPage.msgs.applyFail')) }
}

const applyToJoinSpace = async () => {
  try {
    if (!loginUserStore.loginUser?.id) {
      message.error(t('pages.spaceDetailPage.msgs.notLoggedIn'))
      router.push({ name: 'UserLogin' })
      return
    }
    await updateUserSpaceStatus()
    if (userSpaceStatus.value.hasPending) return message.info(t('pages.spaceDetailPage.msgs.pendingAdmin'))
    if (userSpaceStatus.value.isMember) return message.info(t('pages.spaceDetailPage.msgs.alreadyMember'))
    showJoinSpaceDialog.value = true
  } catch (error) { message.error(t('pages.spaceDetailPage.msgs.systemError')) }
}

const userSpaceStatus = ref({ isMember: false, hasPending: false, application: null })

const updateUserSpaceStatus = async () => {
  if (!props.id || !loginUserStore.loginUser?.id) {
    userSpaceStatus.value = { isMember: false, hasPending: false, application: null }
    return
  }
  try {
    if (space.value.spaceType === SPACE_TYPE_ENUM.PRIVATE && space.value.userId === loginUserStore.loginUser.id) {
      userSpaceStatus.value = { isMember: true, hasPending: false, application: null }
      return
    }
    const res = await checkPendingApplicationUsingPost({ userId: loginUserStore.loginUser.id, spaceId: props.id })
    if (res.data.code === 0) {
      const application = res.data.data
      if (application) {
        userSpaceStatus.value = { isMember: application.status === 1, hasPending: application.status === 0, application }
      } else {
        userSpaceStatus.value = { isMember: false, hasPending: false, application: null }
      }
    } else {
      userSpaceStatus.value = { isMember: false, hasPending: false, application: null }
    }
  } catch (error) {
    userSpaceStatus.value = { isMember: false, hasPending: false, application: null }
  }
}

const cancelJoin = () => { showJoinSpaceDialog.value = false }

const fetchSpaceDetail = async () => {
  activeNavTab.value = 'dynamics'
  activeMobileBarTab.value = 'content'
  loading.value = true
  try {
    if (!props.id) {
      space.value = {} as API.SpaceVO
      userSpaceStatus.value = { isMember: false, hasPending: false, application: null }
      loading.value = false
      isStatusLoading.value = false
      if (device.value !== DEVICE_TYPE_ENUM.PC) {
        showMobileSpaceSheet.value = true
      }
      return
    }

    const res = await getSpaceVoByIdUsingGet({ id: props.id })
    if (res.data.code === 0 && res.data.data) {
      space.value = res.data.data
      await updateUserSpaceStatus()
      if (canManageSpaceUser.value) await fetchPendingApplications()

      if (userSpaceStatus.value.isMember || isPublicPreview.value) {
        await Promise.all([
          fetchSpacePosts(),
          space.value.spaceType !== SPACE_TYPE_ENUM.PRIVATE ? fetchMemberList() : Promise.resolve(),
          space.value.spaceType !== SPACE_TYPE_ENUM.PRIVATE ? fetchActivities() : Promise.resolve()
        ])
      }
      if (mySpaceList.value.length === 0) {
        fetchMySpacesList()
      }
    } else if (res.data.code === 40101) {
      await checkPendingApplication()
      return
    }
  } catch (e: any) {
    console.error('fetchSpaceDetail error:', e)
  } finally {
    loading.value = false
    isStatusLoading.value = false
  }
}

const dataList = ref<API.PictureVO[]>([])
const total = ref(0)
const loading = ref(true)

const searchParams = reactive<API.PictureQueryRequest>({
  current: 1, pageSize: 12, sortField: 'createTime', sortOrder: 'descend', orderBy: ['createTime DESC', 'id DESC'],
})

const loadingMore = ref(false)
const hasMore = ref(true)

const startY = ref(0)
const currentY = ref(0)
const refreshDistance = ref(0)
const isRefreshing = ref(false)
const maxPullDistance = 100
const refreshThreshold = 80

const handleMobileBack = () => {
  if (window.history.length > 1 && window.history.state?.back) {
    router.back()
  } else {
    router.push('/')
  }
}

const isHeaderSticky = ref(false)
const scrollTimer = ref<number | null>(null)

const handleMainScroll = (e?: Event) => {
  const target = (e?.target as HTMLElement)
  const contentWrap = contentWrapRef.value
  const spacePage = spacePageRef.value

  let scrollTop = window.pageYOffset || document.documentElement.scrollTop || document.body.scrollTop || 0
  let scrollHeight = Math.max(document.documentElement.scrollHeight, document.body.scrollHeight || 0)
  let clientHeight = window.innerHeight || document.documentElement.clientHeight || 0

  const activeContainer = [target, contentWrap, spacePage].find(
    el => el && typeof el.scrollTop === 'number' && el.scrollHeight > el.clientHeight + 10
  )

  if (activeContainer) {
    scrollTop = activeContainer.scrollTop
    scrollHeight = activeContainer.scrollHeight
    clientHeight = activeContainer.clientHeight
  }

  isHeaderSticky.value = scrollTop > 110

  if (scrollTimer.value !== null) return
  scrollTimer.value = window.setTimeout(() => {
    let curScrollTop = window.pageYOffset || document.documentElement.scrollTop || document.body.scrollTop || 0
    let curScrollHeight = Math.max(document.documentElement.scrollHeight, document.body.scrollHeight || 0)
    let curClientHeight = window.innerHeight || document.documentElement.clientHeight || 0

    const activeContainerInner = [target, contentWrap, spacePage].find(
      el => el && typeof el.scrollTop === 'number' && el.scrollHeight > el.clientHeight + 10
    )

    if (activeContainerInner) {
      curScrollTop = activeContainerInner.scrollTop
      curScrollHeight = activeContainerInner.scrollHeight
      curClientHeight = activeContainerInner.clientHeight
    }

    const distanceToBottom = curScrollHeight - curScrollTop - curClientHeight

    if (distanceToBottom < 350) {
      if (activeNavTab.value === 'dynamics' && !spacePostsLoading.value && !spacePostsLoadingMore.value && spacePostsHasMore.value) {
        loadMoreSpacePosts()
      } else if (activeNavTab.value === 'pictures' && !loading.value && !loadingMore.value && hasMore.value) {
        loadMore()
      } else if (activeNavTab.value === 'activities' && !activitiesLoading.value && !activitiesLoadingMore.value && activitiesHasMore.value) {
        loadMoreActivities()
      } else if (activeNavTab.value === 'members' && !membersLoading.value && !membersLoadingMore.value && membersHasMore.value) {
        loadMoreMembers()
      }
    }

    if (scrollTimer.value !== null) {
      clearTimeout(scrollTimer.value)
      scrollTimer.value = null
    }
  }, 100)
}

const handleSpacePageScroll = (event: Event) => {
  const target = event.currentTarget as HTMLElement | null
  if (target && !scrollPositionFrozen.value) savedScrollPosition.value = target.scrollTop
  handleMainScroll(event)
}

watch(activeNavTab, (newTab) => {
  if (newTab === 'dynamics' && spacePosts.value.length === 0) {
    fetchSpacePosts()
  }
  if (newTab === 'pictures' && dataList.value.length === 0) {
    fetchData()
  }
  if (newTab === 'members' && paginatedMembers.value.length === 0) {
    fetchMemberList()
  }
  if (newTab === 'activities' && activities.value.length === 0) {
    fetchActivities()
  }
})

const checkScrollBottom = (e?: Event) => {
  handleMainScroll(e)
}

const loadMore = async () => {
  if (loadingMore.value || !hasMore.value || loading.value) return
  loadingMore.value = true
  try {
    if (!userSpaceStatus.value.isMember && !isPublicPreview.value) {
      await updateUserSpaceStatus()
      if (!userSpaceStatus.value.isMember && !userSpaceStatus.value.hasPending) await checkPendingApplication()
      if (!userSpaceStatus.value.isMember) return
    }
    const nextPage = Math.floor(dataList.value.length / searchParams.pageSize) + 1
    let res: any;
    if (searchParams.searchText) {
      res = await searchPictureBySemanticUsingPost({ searchText: searchParams.searchText, spaceId: props.id as any, current: nextPage, pageSize: searchParams.pageSize })
    } else {
      res = await listPictureVoByPageUsingPost({ ...searchParams, current: nextPage, spaceId: props.id, orderBy: ['createTime DESC', 'id DESC'] })
    }
    if (res.data?.code === 0) {
      const newRecords = res.data.data?.records ?? []
      const totalCount = Number(res.data.data?.total) || 0
      total.value = totalCount
      if (newRecords.length > 0) {
        dataList.value = [...dataList.value, ...newRecords]
      }
      hasMore.value = dataList.value.length < totalCount && newRecords.length > 0
    } else {
      hasMore.value = false
    }
  } catch (error) {
    console.error('加载更多失败:', error)
  } finally {
    loadingMore.value = false
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    if (!props.id) { await router.push({ name: 'Home' }); return }
    if (!userSpaceStatus.value.isMember && !isPublicPreview.value) {
      await updateUserSpaceStatus()
      if (!userSpaceStatus.value.isMember && !userSpaceStatus.value.hasPending) await checkPendingApplication()
      if (!userSpaceStatus.value.isMember) return
    }
    searchParams.current = 1
    let res: any;
    if (searchParams.searchText) {
      res = await searchPictureBySemanticUsingPost({ searchText: searchParams.searchText, spaceId: props.id as any, current: 1, pageSize: searchParams.pageSize })
    } else {
      res = await listPictureVoByPageUsingPost({ ...searchParams, current: 1, spaceId: props.id, orderBy: ['createTime DESC', 'id DESC'] })
    }
    if (res.data?.code === 0) {
      const records = res.data.data?.records ?? []
      const totalCount = Number(res.data.data?.total) || 0
      dataList.value = records
      total.value = totalCount
      hasMore.value = dataList.value.length < totalCount && records.length > 0
    }
  } catch (error) {
    console.error('获取数据失败:', error)
  } finally {
    loading.value = false
  }
}





const onColorChange = async (color: string) => {
  if (!props.id) return
  spaceSearchLoading.value = true
  try {
    const res = await searchPictureByColorUsingPost({ picColor: color, spaceId: String(props.id) as any })
    if (res.data.code === 0) applyPictureSearchResults(res.data.data || [])
  } catch {
    message.error(t('pages.spaceDetailPage.msgs.colorSearchFail'))
  } finally {
    spaceSearchLoading.value = false
  }
}

const onSearch = (newSearchParams: API.PictureQueryRequest) => {
  if (!newSearchParams) return
  Object.assign(searchParams, { ...newSearchParams, current: 1, pageSize: searchParams.pageSize, sortField: 'createTime', sortOrder: 'descend' })
  fetchData()
}

const onPictureSearch = async (imageUrl: string) => {
  if (!imageUrl) return
  if (!props.id) return
  spaceSearchLoading.value = true
  try {
    const res = await searchPictureByPictureUsingPost({ imageUrl, spaceId: String(props.id) as any, current: 1, pageSize: 20 })
    if (res.data.code === 0) applyPictureSearchResults(res.data.data?.records || [])
    else message.error(t('pages.spaceDetailPage.msgs.imageSearchFail'))
  } catch {
    message.error(t('pages.spaceDetailPage.msgs.imageSearchFail'))
  } finally {
    spaceSearchLoading.value = false
  }
}
const getScrollTop = () => {
  const container = document.getElementById('spaceDetailPage')
  const containerScrollTop = container ? container.scrollTop : 0
  const windowScrollTop = window.scrollY || document.documentElement.scrollTop || document.body.scrollTop || 0
  return Math.max(containerScrollTop, windowScrollTop)
}

const isCanPull = ref(false)

const handleTouchStart = (e: TouchEvent) => {
  if (device.value === DEVICE_TYPE_ENUM.PC) return
  if (getScrollTop() > 5) {
    isCanPull.value = false
    return
  }
  isCanPull.value = true
  startY.value = e.touches[0].clientY
  currentY.value = e.touches[0].clientY
}

const handleTouchMove = (e: TouchEvent) => {
  if (device.value === DEVICE_TYPE_ENUM.PC || isRefreshing.value || !isCanPull.value) return
  if (getScrollTop() > 5) {
    refreshDistance.value = 0
    isCanPull.value = false
    return
  }
  currentY.value = e.touches[0].clientY
  const distance = currentY.value - startY.value
  if (distance > 0) {
    if (e.cancelable) e.preventDefault()
    refreshDistance.value = Math.min(distance * 0.5, maxPullDistance)
    document.body.style.overflow = 'hidden'
  }
}

const handleTouchEnd = async () => {
  document.body.style.overflow = ''
  if (device.value === DEVICE_TYPE_ENUM.PC) return
  if (refreshDistance.value >= refreshThreshold && !isRefreshing.value) {
    isRefreshing.value = true
    refreshDistance.value = 0
    await onRefresh()
    isRefreshing.value = false
  }
  refreshDistance.value = 0
}

const onRefresh = async () => {
  try {
    searchFormRef.value?.handleRefresh()
    searchParams.current = 1
    hasMore.value = true

    // 并行加载图片列表和轮播图数据 (不提前清空数据，实现静默无缝刷新，彻底防止高度骤降坍缩)
    await Promise.all([
      fetchData(),
      space.value.spaceType !== SPACE_TYPE_ENUM.PRIVATE ? fetchActivities() : Promise.resolve()
    ])
  } catch (error) {
  } finally {
    loading.value = false
  }
}

const initSpaceState = () => {
  dataList.value = []
  activities.value = []
  total.value = 0
  loading.value = true
  hasMore.value = true
  Object.assign(searchParams, { current: 1, pageSize: 12, sortField: 'createTime', sortOrder: 'descend', searchText: '', category: '', tags: [], orderBy: ['createTime DESC', 'id DESC'] })
  if (searchFormRef.value) {
    searchFormRef.value.clearSearchState()
  }
}

watch(() => props.id, async (newSpaceId, oldSpaceId) => {
  if (newSpaceId !== oldSpaceId) {
    if (oldSpaceId) initSpaceState()

    // 清载成员列表相关状态
    spaceMemberList.value = []
    paginatedMembers.value = []
    memberParams.current = 1
    membersHasMore.value = true

    isStatusLoading.value = true
    activitiesLoading.value = true
    await fetchSpaceDetail()
    isStatusLoading.value = false
  }
}, { immediate: true })

watch(activeNavTab, (newTab) => {
  if (newTab === 'members' && props.id) {
    fetchMemberList(false)
  }
  if (newTab === 'activities' && props.id) {
    fetchActivities()
  }
})

const bindMainScrollListener = () => {
  window.removeEventListener('scroll', handleMainScroll)
  window.addEventListener('scroll', handleMainScroll, { passive: true })
}

const unbindMainScrollListener = () => {
  window.removeEventListener('scroll', handleMainScroll)
}

onMounted(bindMainScrollListener)

onUnmounted(() => {
  unbindMainScrollListener()
  if (scrollTimer.value !== null) {
    clearTimeout(scrollTimer.value)
    scrollTimer.value = null
  }
})

const formatSize = (size?: number) => {
  if (!size) return '0 B'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
  if (size < 1024 * 1024 * 1024) return (size / (1024 * 1024)).toFixed(2) + ' MB'
  return (size / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
}

// 是否超额使用（会员到期降级导致 usedStorage > maxStorage；公开空间不限容量不提示）
const isOverQuota = computed(() => {
  if (space.value.visibility === SPACE_VISIBILITY_ENUM.PUBLIC) return false
  if (!space.value.maxStorage) return false
  const used = space.value.usedStorage ?? 0
  const max = space.value.maxStorage
  return used > max
})

const capacityPercentage = computed(() => {
  if (!space.value.maxStorage) return 0
  const used = space.value.usedStorage ?? 0
  const max = space.value.maxStorage
  return Math.min(100, Math.round((used / max) * 100))
})

const gaugeOption = computed(() => {
  const percentage = capacityPercentage.value
  const remaining = 100 - percentage
  return {
    series: [{
      type: 'pie',
      radius: ['70%', '90%'],
      center: ['50%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 4, borderColor: 'transparent', borderWidth: 0 },
      label: {
        show: true, position: 'center', formatter: `{d|${percentage}%}`,
        rich: { d: { fontSize: 14, fontWeight: 'bold', color: '#3b82f6', lineHeight: 20 } }
      },
      labelLine: { show: false },
      data: [
        { value: percentage, name: t('pages.spaceDetailPage.info.used'), itemStyle: { color: '#3b82f6' } },
        { value: remaining, name: t('pages.spaceDetailPage.info.remaining'), itemStyle: { color: 'rgba(150, 150, 150, 0.2)' } }
      ]
    }]
  }
})
</script>

<style scoped>
.yuemu-spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid currentColor;
  border-top-color: transparent;
  border-radius: 50%;
  animation: yuemu-spin 0.8s linear infinite;
  flex-shrink: 0;
}
@keyframes yuemu-spin {
  to { transform: rotate(360deg); }
}
.apple-page-container {
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  min-height: 100vh;
  color: var(--text-primary);
  transition: var(--theme-transition);
}

#spaceDetailPage {
  width: 100%;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  overscroll-behavior-y: none;
}

#spaceDetailPage.is-pulling {
  overflow: hidden;
  height: 100vh;
}

.glass-header {
  position: sticky;
  top: 0;
  z-index: 50;
  background: var(--header-background);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 32px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.03);
  border-bottom: 1px solid rgba(0, 0, 0, 0.02);
}

.search-form-container {
  flex: 1;
  margin-right: 12px;
}

.search-form-container :deep(.picture-search-form) {
  padding: 0;
  margin: 0;
}

.icon-action-btn {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: none;
  background: var(--hover-background);
  color: var(--text-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  cursor: pointer;
  transition: var(--theme-transition);
}
.icon-action-btn:hover {
  transform: scale(1.05);
  background: var(--border-color);
}

.top-carousel-section {
  margin-bottom: 8px;
  padding: 0 16px;
}
.carousel-wrapper {
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 8px 24px var(--shadow-color);
  position: relative;
}
.carousel-wrapper :deep(.luxury-carousel) {
  height: 400px;
  max-height: 400px;
}



.permission-denied-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  padding: 20px;
}
.denied-content {
  background: var(--card-background);
  padding: 40px;
  border-radius: 24px;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.04);
  text-align: center;
  max-width: 400px;
  width: 100%;
}
.lock-icon-wrap {
  width: 64px;
  height: 64px;
  background: var(--hover-background);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  color: var(--text-secondary);
  font-size: 28px;
}
.denied-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 12px;
  color: var(--text-primary);
}
.denied-desc {
  font-size: 15px;
  color: var(--text-secondary);
  margin: 0 0 32px;
  line-height: 1.5;
}
.apple-primary-btn {
  width: 100%;
  padding: 14px;
  border-radius: 12px;
  background: var(--text-primary, #1e293b);
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: var(--theme-transition);
}
.apple-primary-btn:hover { background: #334155; transform: scale(0.98); }
.apple-primary-btn.is-pending { background: var(--hover-background); color: var(--text-secondary); cursor: not-allowed; }
.apple-primary-btn.is-pending:hover { transform: none; }

.apple-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}

/* 公开空间预览模式提示条 */
/* 超额用量警告条 */
.over-quota-warning-banner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin: 14px 16px;
  padding: 12px 20px;
  background: rgba(245, 158, 11, 0.06);
  border: 1px solid rgba(245, 158, 11, 0.15);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  color: #92400e;
  font-size: 13px;
  font-weight: 500;
  border-radius: 14px;
  flex-wrap: wrap;
  text-align: center;
  box-shadow: 0 4px 14px rgba(245, 158, 11, 0.05);
  line-height: 1.6;
}
.over-quota-warning-banner .yuemu-icon-wrapper {
  color: #d97706;
  flex-shrink: 0;
}
.over-quota-warning-banner strong {
  color: #b45309;
  font-weight: 600;
}

.public-preview-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin: 12px 16px;
  padding: 10px 14px;
  background: rgba(37, 99, 235, 0.06);
  border: 1px solid rgba(37, 99, 235, 0.16);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  color: #1e40af;
  font-size: 12px;
  font-weight: 500;
  border-radius: 12px;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.04);
}
.public-preview-banner > span {
  flex: 1;
  text-align: left;
  line-height: 1.4;
}
.public-preview-banner .preview-apply-btn {
  padding: 5px 15px;
  border-radius: 999px;
  background: #2563eb;
  color: #ffffff;
  font-size: 12px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
.public-preview-banner .preview-apply-btn:hover {
  background: #1d4ed8;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.35);
}
.public-preview-banner .preview-apply-btn.is-pending {
  opacity: 0.7;
  background: #94a3b8;
  box-shadow: none;
}

/* 空间公告条（单行贯通，暖金色调） */
.space-announcement-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 12px 0;
  padding: 8px 14px;
  background: linear-gradient(135deg, rgba(251, 191, 36, 0.1), rgba(245, 158, 11, 0.06));
  border: 1px solid rgba(245, 158, 11, 0.2);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(245, 158, 11, 0.06);
}
.space-announcement-bar .announcement-icon {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: #ffffff;
  font-size: 13px;
}
.space-announcement-bar .announcement-text-single {
  flex: 1;
  min-width: 0;
  font-size: 13.5px;
  font-weight: 500;
  color: var(--text-primary, #0f172a);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.space-announcement-bar .announcement-time-single {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--text-secondary, #94a3b8);
  margin-left: auto;
}
.space-announcement-bar .announcement-admin-hint {
  font-size: 12px;
  color: var(--text-tertiary);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.space-announcement-bar .announcement-admin-hint.is-admin {
  color: #7c3aed;
}
.announcement-modal-tip {
  margin-top: 10px;
  font-size: 12px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 活跃成员榜 */
.space-active-members-panel {
  margin: 12px 16px;
  padding: 16px;
  background: #ffffff;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
}
.active-members-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.active-members-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}
.active-members-title-wrap .rank-star-icon {
  color: #f59e0b;
  font-size: 15px;
}
.active-members-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}
.active-members-sub {
  font-size: 12px;
  color: var(--text-tertiary);
}
.active-members-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.active-member-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.02);
  cursor: pointer;
  transition: all 0.2s ease;
}
.active-member-card:hover {
  background: rgba(99, 102, 241, 0.06);
}
.rank-badge {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: #ffffff;
  background: #94a3b8;
}
.rank-badge.rank-1 {
  background: linear-gradient(135deg, #f59e0b, #fbbf24);
}
.rank-badge.rank-2 {
  background: linear-gradient(135deg, #94a3b8, #cbd5e1);
}
.rank-badge.rank-3 {
  background: linear-gradient(135deg, #d97706, #f59e0b);
}
.active-member-avatar {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
}
.active-member-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.active-member-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.active-member-role {
  font-size: 12px;
  color: var(--text-tertiary);
}
.active-member-score {
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 700;
  color: #6366f1;
  display: flex;
  align-items: center;
  gap: 4px;
}
.active-member-score i {
  color: #f59e0b;
  font-size: 12px;
}
.empty-icon-wrap {
  font-size: 56px;
  color: var(--border-color);
  margin-bottom: 20px;
}
.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px;
}
.empty-desc {
  font-size: 14px;
  color: var(--text-secondary);
}

.floating-upload-btn {
  position: fixed;
  bottom: 120px;
  right: 32px;
  width: 56px;
  height: 56px;
  border-radius: 28px;
  background: var(--link-color);
  color: white;
  border: none;
  font-size: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px var(--shadow-color);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  z-index: 90;

  animation: shadowBreathe 3s ease-in-out infinite;
}
@keyframes shadowBreathe {
  0% { box-shadow: 0 4px 12px rgba(22, 119, 255, 0.2); transform: scale(1); }
  50% { box-shadow: 0 12px 28px rgba(22, 119, 255, 0.45); transform: scale(1.03); }
  100% { box-shadow: 0 4px 12px rgba(22, 119, 255, 0.2); transform: scale(1); }
}
.floating-upload-btn:hover { transform: translateY(-4px) scale(1.05); }
.floating-upload-btn:active { transform: scale(0.95); }

.content-wrap {
  flex: 1;
  margin: auto;
  padding: 0 16px;
  width: 100%;
  box-sizing: border-box;
}

.custom-pull-refresh {
  min-height: 70vh;
  position: relative;
}

.refresh-indicator {
  position: absolute;
  top: -60px;
  left: 0;
  right: 0;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 14px;
  transition: opacity 0.2s;
}

.refresh-icon {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-icon {
  font-size: 18px;
  color: var(--link-color);
}

.pull-arrow {
  font-size: 20px;
  transition: transform 0.3s;
  color: var(--link-color);
}

.refresh-text {
  font-weight: 500;
}

.apple-overlay {
  position: fixed;
  inset: 0;
  background: var(--upload-sheet-backdrop);
  backdrop-filter: blur(4px);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.2s ease;
}
.apple-modal-sheet {
  background: var(--card-background);
  width: 90%;
  max-width: 440px;
  border-radius: 24px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 40px var(--shadow-color);
  animation: scaleUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
.drag-indicator { display: none; }
.modal-header-compact {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 16px;
}
.modal-title { font-size: 18px; font-weight: 700; margin: 0; color: var(--text-primary); }
.modal-close-icon {
  width: 30px; height: 30px; border-radius: 15px; background: var(--hover-background);
  color: var(--text-secondary); border: none; display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: 0.2s;
}
.modal-close-icon:hover { background: var(--border-color); color: var(--text-primary); }
.modal-scroll-body {
  padding: 20px 24px;
  max-height: 70vh;
  overflow-y: auto;
}

.info-card {
  background: var(--hover-background);
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 16px;
}
.info-row { display: flex; justify-content: space-between; padding: 8px 0;  }
.info-row:last-child { border-bottom: none; padding-bottom: 0; }
.info-row:first-child { padding-top: 0; }
.desc-row { flex-direction: column; gap: 4px; }
.info-label { font-size: 14px; color: var(--text-secondary); }
.info-value { font-size: 14px; color: var(--text-primary); font-weight: 500; text-align: right;}
.desc-text { text-align: left; line-height: 1.4; color: var(--text-secondary); }

.capacity-card { display: flex; justify-content: space-between; align-items: center; }
.card-title { font-size: 14px; color: var(--text-secondary); margin-bottom: 8px; display: block;}
.num-block { margin-bottom: 4px; }
.num-block .current { font-size: 18px; font-weight: 700; color: var(--link-color); }
.num-block .current.count { color: var(--markdown-heading-green-border); }
.num-block .total { font-size: 12px; color: var(--text-secondary); margin-left: 4px; }

.action-grid-card { margin-top: 8px; }
.action-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}
.grid-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px 0;
  transition: opacity 0.2s;
}
.grid-item:hover { opacity: 0.7; }
.icon-wrap {
  width: 48px; height: 48px; border-radius: 16px;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; color: #fff; position: relative;
}
.bg-blue { background: var(--link-color); }
.bg-green { background: #34c759; }
.bg-purple { background: #5856d6; }
.bg-cyan { background: #32ade6; }
.bg-orange { background: #ff9500; }
.bg-gray { background: #8e8e93; }
.red-dot {
  position: absolute; top: -2px; right: -2px; width: 12px; height: 12px;
  background: #ff3b30; border: 2px solid var(--card-background); border-radius: 50%;
}
.grid-item span { font-size: 12px; color: var(--text-primary); font-weight: 500; }

.apple-danger-btn {
  width: 100%; padding: 14px; border-radius: 12px;
  background: rgba(255, 59, 48, 0.1); color: #ff3b30; font-size: 15px; font-weight: 600;
  border: none; cursor: pointer; transition: 0.2s;
}
.apple-danger-btn:hover { background: rgba(255, 59, 48, 0.15); }

.form-body { display: flex; flex-direction: column; gap: 16px; }
.apple-input-group label { display: block; font-size: 13px; font-weight: 600; color: var(--text-secondary); margin-bottom: 6px; padding-left: 4px; }
.apple-input-group input, .apple-input-group textarea {
  width: 100%; background: var(--hover-background); border: 1px solid transparent; border-radius: 12px;
  padding: 12px 16px; font-size: 15px; color: var(--text-primary); transition: all 0.2s; outline: none; box-sizing: border-box;
}
.apple-input-group input:focus, .apple-input-group textarea:focus { background: var(--card-background); border-color: var(--link-color); }
.cover-upload-box {
  width: 100%; height: 120px; background: var(--hover-background); border-radius: 12px;
  display: flex; align-items: center; justify-content: center; overflow: hidden; cursor: pointer; position: relative;
}
.cover-upload-box img { width: 100%; height: 100%; object-fit: cover; }
.upload-placeholder { display: flex; flex-direction: column; align-items: center; gap: 8px; color: var(--text-secondary); }
.upload-placeholder i { font-size: 24px; }
.upload-loading { position: absolute; inset: 0; background: var(--header-background); display: flex; align-items: center; justify-content: center; }

.modal-footer-actions { display: flex; gap: 12px; margin-top: 8px; }
.apple-btn-secondary, .apple-btn-primary { flex: 1; padding: 14px; border-radius: 12px; font-size: 15px; font-weight: 600; border: none; cursor: pointer; transition: 0.2s; }
.apple-btn-secondary { background: var(--hover-background); color: var(--text-primary); }
.apple-btn-secondary:hover { background: var(--border-color); }
.apple-btn-primary { background: var(--link-color); color: #fff; }
.apple-btn-primary:hover { background: var(--link-hover-color); }

.apple-dialog {
  background: var(--card-background); width: 300px; border-radius: 18px;
  display: flex; flex-direction: column; text-align: center;
  box-shadow: 0 20px 40px var(--shadow-color); animation: scaleUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
.dialog-title { font-size: 17px; font-weight: 600; color: var(--text-primary); margin: 20px 0 8px; }
.dialog-desc { font-size: 13px; color: var(--text-secondary); margin: 0 20px 20px; line-height: 1.4; }
.dialog-actions { display: flex; border-top: 1px solid var(--border-color); }
.dialog-btn { flex: 1; padding: 14px 0; background: transparent; border: none; font-size: 16px; font-weight: 500; cursor: pointer; transition: 0.2s; color: var(--text-primary); }
.dialog-btn:active { background: var(--hover-background); }
.dialog-btn.cancel { color: var(--text-secondary); border-right: 1px solid var(--border-color); }
.dialog-btn.danger { color: #ff3b30; font-weight: 600;}
.dialog-btn.confirm { color: var(--link-color); font-weight: 600;}

@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes scaleUp { from { transform: scale(0.95); opacity: 0; } to { transform: scale(1); opacity: 1; } }
@keyframes slideUp { from { transform: translateY(100%); } to { transform: translateY(0); } }

@media screen and (max-width: 768px) {
  .apple-page-container { background: var(--background); }
  .glass-header { padding: 16px 20px; gap: 12px; }
  .search-form-container { margin-right: 0; min-width: 0; }
  .icon-action-btn { width: 38px; height: 38px; font-size: 14px; flex-shrink: 0; }
  .top-carousel-section { padding: 0 4px; }
  .content-wrap { padding: 0 4px; }
  .carousel-wrapper :deep(.luxury-carousel) { height: 220px; max-height: 220px; }



  .apple-modal-sheet {
    position: absolute; bottom: 0; left: 0;
    width: 100%; max-width: 100%;
    border-radius: 24px 24px 0 0;
    animation: slideUp 0.35s cubic-bezier(0.16, 1, 0.3, 1);
  }
  .drag-indicator {
    display: block; width: 42px; height: 5px; border-radius: 2.5px;
    background: var(--border-color); margin: 10px auto 0;
  }
  .modal-header-compact { padding: 12px 20px 12px; border-bottom: none;}
  .modal-scroll-body { padding: 10px 20px 30px; max-height: 80vh; }

  .floating-upload-btn { right: 20px; bottom: 120px; width: 50px; height: 50px; font-size: 20px;}
}
.space-list-loading-more, .space-list-no-more {
  text-align: center;
  padding: 16px 0 32px;
  color: var(--text-secondary);
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.space-list-loading-more .yuemu-spinner {
  color: var(--link-color);
}

.search-bar-fake {
  width: 100%;
  height: 40px;
  background: var(--hover-background);
  border-radius: 20px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  color: var(--text-secondary);
  font-size: 15px;
  cursor: pointer;
  transition: all 0.2s;
}
.search-bar-fake:active {
  background: var(--border-color);
}
.search-bar-fake i {
  margin-right: 8px;
  font-size: 16px;
}

.fab-radial-container {
  position: fixed;
  bottom: 120px;
  right: 32px;
  width: 56px;
  height: 56px;
  z-index: 195;
}
.radial-menu {
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  pointer-events: none;
}
.radial-item {
  position: absolute;
  top: 6px;
  left: 6px;
  width: 44px;
  height: 44px;
  border: none;
  background: transparent;
  padding: 0;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 195;
  transition: transform 0.4s cubic-bezier(0.68, -0.55, 0.265, 1.55), opacity 0.3s;
  transform: translate(0, 0) scale(0);
  opacity: 0;
  pointer-events: none;
}
.radial-menu.is-expanded .radial-item {
  pointer-events: auto;
}
.radial-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--card-background);
  color: var(--text-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  transition: transform 0.2s;
}
.radial-item:active .radial-icon {
  transform: scale(0.9);
}
.radial-label {
  position: absolute;
  bottom: -22px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 12px;
  color: var(--text-primary);
  background: var(--card-background);
  padding: 2px 6px;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  white-space: nowrap;
  opacity: 0;
  transition: opacity 0.2s;
  pointer-events: none;
}
.radial-menu.is-expanded .radial-label {
  opacity: 1;
  transition-delay: 0.2s;
}
.radial-menu.is-expanded .item-0 { transform: translate(0px, -110px) scale(1); opacity: 1; transition-delay: 0.05s; }
.radial-menu.is-expanded .item-1 { transform: translate(-78px, -78px) scale(1); opacity: 1; transition-delay: 0.1s; }
.radial-menu.is-expanded .item-2 { transform: translate(-110px, 0px) scale(1); opacity: 1; transition-delay: 0.15s; }

.floating-upload-btn {
  position: absolute;
  top: 0; left: 0;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: var(--link-color);
  color: #fff;
  border: none;
  font-size: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.4);
  cursor: pointer;
  z-index: 200;
  transition: transform 0.2s, box-shadow 0.2s;
}
.floating-upload-btn:active {
  transform: scale(0.95);
}
@media screen and (min-width: 769px) {
  .fab-radial-container {
    width: 64px !important; height: 64px !important;
    right: 48px !important; bottom: 80px !important; /* PC 端留出底边距 */
  }
  /* 微调 PC 端内部项使其居中 (64 - 44 = 20, 20/2 = 10) */
  .radial-item { top: 10px !important; left: 10px !important; }
}

.fab-mask {
  position: fixed;
  inset: 0;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px);
  z-index: 190;
}

.full-search-popup {
  display: flex;
  flex-direction: column;
  background: var(--background);
}
.search-popup-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: var(--header-background);
  border-bottom: 1px solid var(--border-color);
  gap: 12px;
}
.back-btn {
  background: none;
  border: none;
  font-size: 20px;
  color: var(--text-primary);
  padding: 4px;
  cursor: pointer;
}
.search-popup-header .search-form-container {
  flex: 1;
  margin: 0;
}
.search-popup-body {
  flex: 1 !important;
  overflow-y: auto !important;
  padding: 20px !important;
  background: var(--card-background, #ffffff) !important;
}

@media screen and (max-width: 768px) {
  .fab-radial-container { right: 20px; bottom: 120px; }
}

.top-header-right-user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pc-header-share-btn {
  height: 38px;
  padding: 0 16px;
  border-radius: 20px;
  border: 1px solid var(--border-color, #e2e8f0);
  background: var(--card-background, #ffffff);
  color: var(--text-primary, #1e293b);
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.pc-header-share-btn:hover {
  background: #2563eb;
  color: #ffffff;
  border-color: #2563eb;
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.3);
}

.pc-header-share-btn i {
  font-size: 14px;
}


.fake-search-bar {
  width: 100%; max-width: 600px; margin: 0 auto; height: 48px;
  background: var(--card-background, #ffffff);
  border-radius: 24px;
  display: flex; align-items: center; justify-content: flex-start;
  padding: 0 20px;
  font-size: 15px; color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.25, 0.8, 0.25, 1);
  border: 1px solid var(--border-color, #e5e7eb);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}
.fake-search-bar:active, .fake-search-bar:hover {
  background: #f8fafc;
  border-color: var(--link-color);
  box-shadow: 0 6px 16px rgba(22, 119, 255, 0.12);
}
.fake-search-bar i {
  margin-right: 12px; font-size: 17px;
  color: var(--link-color, #2563eb);
}
.fake-search-bar span {
  font-weight: 500;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.van-overlay {
  display: none !important;
  opacity: 0 !important;
  pointer-events: none !important;
}

/* 搜索弹窗全屏遮罩 */
.full-search-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 20005;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.full-search-popup {
  display: flex !important;
  flex-direction: column !important;
  background-color: #ffffff !important;
  box-sizing: border-box !important;
  overflow: hidden !important;
  border: 1px solid var(--border-color, rgba(0, 0, 0, 0.08)) !important;
}

/* 移动端 85vh 底部抽屉 */
.full-search-popup.is-mobile {
  width: 100%;
  max-width: 100vw;
  height: 85vh;
  border-radius: 24px 24px 0 0;
  animation: yuemuSlideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

/* PC 端 760px * 640px 上下水平绝对居中固定尺寸卡片弹窗 (防抖动、防变高变矮) */
.full-search-popup.is-desktop,
.van-popup.full-search-popup.is-desktop {
  position: fixed !important;
  top: 50% !important;
  left: 50% !important;
  transform: translate(-50%, -50%) !important;
  bottom: auto !important;
  right: auto !important;
  margin: 0 !important;
  width: 760px !important;
  max-width: 90vw !important;
  height: 640px !important;
  max-height: 85vh !important;
  border-radius: 24px !important;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.18), 0 0 0 1px rgba(0, 0, 0, 0.06) !important;
  z-index: 20005 !important;
  display: flex !important;
  flex-direction: column !important;
  overflow: hidden !important;
  animation: yuemuPop 0.25s cubic-bezier(0.16, 1, 0.3, 1) !important;
}

.popup-close-icon-btn {
  width: 34px !important;
  height: 34px !important;
  border-radius: 50% !important;
  border: none !important;
  background: rgba(0, 0, 0, 0.05) !important;
  color: var(--text-secondary, #64748b) !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  cursor: pointer !important;
  font-size: 15px !important;
  flex-shrink: 0 !important;
  transition: all 0.2s ease !important;
  margin-left: 4px !important;
}

.popup-close-icon-btn:hover {
  background: rgba(239, 68, 68, 0.1) !important;
  color: #ef4444 !important;
}

.drawer-drag-bar { width: 40px; height: 5px; background: var(--border-color, #e5e7eb); border-radius: 3px; margin: 12px auto 0; flex-shrink: 0; }
.search-popup-header { display: flex !important; align-items: center !important; padding: 14px 18px !important; gap: 12px !important; background: var(--card-background, #ffffff) !important; border-bottom: 1px solid var(--border-color, rgba(0,0,0,0.08)) !important; width: 100% !important; box-sizing: border-box !important; }
.back-btn { background: none; border: none; font-size: 20px; color: var(--text-primary); cursor: pointer; padding: 4px; flex-shrink: 0; }
.search-input-wrap {
  flex: 1; display: flex; align-items: center;
  background: var(--yuemu-input-bg, var(--hover-background));
  border: 1px solid var(--border-color, rgba(0,0,0,0.12));
  border-radius: 12px; padding: 8px 12px; gap: 8px;
  transition: all 0.2s ease;
  min-width: 0;
}
.search-input-wrap:focus-within {
  background: var(--card-background);
  border-color: var(--link-color, #3b82f6);
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.2);
}
.search-input-wrap .yuemu-icon-wrapper { color: var(--link-color, #2563eb); }
.search-input-wrap input { flex: 1; border: none; background: transparent; outline: none; font-size: 15px; color: var(--text-primary); width: 100%; min-width: 0; }
.search-input-wrap input::placeholder { color: var(--text-secondary, #94a3b8); }
.search-btn { background: var(--link-color, #3b82f6); color: #fff; border: none; border-radius: 16px; padding: 8px 16px; font-size: 14px; font-weight: 500; cursor: pointer; flex-shrink: 0; white-space: nowrap; }

.search-popup-body { padding: 24px 16px; flex: 1; overflow-y: auto; background: var(--background); }
.advanced-tools-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 14px; margin-top: 14px; margin-bottom: 24px !important; width: 100%; box-sizing: border-box; }
.tool-card { display: flex; align-items: center; background: var(--card-background); border: 1px solid var(--border-color); border-radius: 16px; padding: 16px; gap: 16px; cursor: pointer; transition: 0.2s; box-shadow: 0 4px 16px rgba(0,0,0,0.02); }
.tool-card:active { transform: scale(0.98); }
.tool-icon { width: 48px; height: 48px; border-radius: 14px; display: flex; align-items: center; justify-content: center; font-size: 20px; color: #fff; }
.bg-gradient-violet { background: linear-gradient(135deg, #8b5cf6 0%, #c084fc 100%); }
.bg-gradient-pink { background: linear-gradient(135deg, #ff0844 0%, #ffb199 100%); }
.bg-gradient-blue { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }
.tool-text { flex: 1; }
.tool-text h4 { margin: 0; font-size: 16px; font-weight: 600; color: var(--text-primary); }
.tool-text p { margin: 4px 0 0; font-size: 13px; color: var(--text-secondary); }
.tool-arrow { color: var(--text-secondary); font-size: 14px; }

@media (max-width: 768px) {
  .advanced-tools-grid {
    grid-template-columns: repeat(3, 1fr) !important;
    gap: 8px !important;
    margin-top: 10px !important;
    margin-bottom: 16px !important;
  }

  .advanced-tools-grid .tool-card {
    padding: 8px 4px !important;
    border-radius: 12px !important;
    justify-content: center !important;
    text-align: center !important;
    gap: 0 !important;
    background: var(--hover-background, #f8fafc) !important;
    border: 1px solid var(--border-color, #e2e8f0) !important;
  }

  .advanced-tools-grid .tool-card .tool-icon,
  .advanced-tools-grid .tool-card p,
  .advanced-tools-grid .tool-card .tool-arrow {
    display: none !important;
  }

  .advanced-tools-grid .tool-card .tool-text h4 {
    font-size: 13px !important;
    font-weight: 600 !important;
    white-space: nowrap !important;
    overflow: hidden !important;
    text-overflow: ellipsis !important;
    text-align: center !important;
  }

  .full-search-popup.is-mobile .search-popup-body {
    padding: 16px 6px !important;
  }
}

.inline-color-picker { margin-top: 24px; background: var(--card-background); border: 1px solid var(--border-color); border-radius: 16px; overflow: hidden; animation: yuemu-scaleUp 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
.picker-header { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border-bottom: 1px solid var(--border-color); font-weight: 600; font-size: 15px; color: var(--text-primary); }
.close-picker { background: var(--hover-background); border: none; width: 28px; height: 28px; border-radius: 14px; color: var(--text-secondary); cursor: pointer; }
.picker-body { padding: 24px; display: flex; justify-content: center; }
.picker-body :deep(.vc-color-wrap) { width: 120px; height: 120px; border-radius: 16px; border: 2px solid var(--border-color); background: var(--hover-background); }

/* PC 端 Codex 风格侧边栏开关按钮 */
.top-header-left-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sidebar-toggle-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  border: 1px solid var(--border-color, #e2e8f0);
  background: var(--card-background, #ffffff);
  color: var(--text-primary, #1e293b);
  font-size: 15px;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.sidebar-toggle-btn .sidebar-layout-icon {
  width: 18px;
  height: 18px;
  stroke: currentColor;
  transition: stroke 0.2s ease;
}

.sidebar-toggle-btn:hover {
  background: var(--hover-background, #f1f5f9);
  border-color: #3b82f6;
  color: #3b82f6;
  transform: translateY(-1px);
}

.sidebar-toggle-btn.is-collapsed {
  color: #2563eb;
  background: rgba(59, 130, 246, 0.12);
  border-color: rgba(59, 130, 246, 0.35);
}

/* PC 端三栏外层框架支持 */
.apple-page-container.pc-layout-active {
  display: flex;
  min-height: auto;
  height: calc(100vh - 90px);
  max-width: 1400px;
  margin: 10px auto;
  background: var(--card-background);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 12px 36px var(--shadow-color);
  transition: var(--theme-transition);
}
.apple-page-container.pc-layout-active #spaceDetailPage {
  flex: 1;
  min-height: auto !important;
  height: 100% !important;
  margin: 0 !important;
  background-color: var(--card-background, #ffffff) !important;
  border: none !important;
  border-radius: 0 !important;
  border-right: 1px solid var(--border-color, rgba(0, 0, 0, 0.08)) !important;
  border-left: 1px solid var(--border-color, rgba(0, 0, 0, 0.08)) !important;
  overflow-y: auto !important;
  min-width: 0 !important;
  box-shadow: none !important;
}

/* PC 端高保真 Banner 标头样式 (封面标题正常参与滚动) */
.yuemu-pc-banner-header {
  padding: 20px 32px 0px !important;
  background: var(--card-background, #ffffff) !important;
}

/* 统一随动滚动与内容区平铺 */
#spaceDetailPage .content-wrap {
  overflow: visible !important;
  height: auto !important;
  flex: 1;
  background-color: var(--chat-background, #f5f5f7);
}

#spaceDetailPage .content-wrap .custom-pull-refresh {
  min-height: auto !important;
}

.banner-search-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 28px;
  position: relative;
  width: 100%;
}

.top-header-right-user {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.header-user-avatar-wrap {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  border: 1.5px solid #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: transform 0.2s ease;
}

.header-user-avatar-wrap:hover {
  transform: scale(1.06);
}

.top-user-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.pc-search-capsule {
  flex: 1;
  height: 42px;
  background: var(--bg-secondary, #f6f6f8);
  border-radius: 21px;
  display: flex;
  align-items: center;
  padding: 0 18px;
  cursor: pointer;
  border: 1px solid var(--border-color, transparent);
  transition: all 0.2s ease;
}
.pc-search-capsule:hover {
  background: var(--bg-hover, #ffffff);
  border-color: var(--border-color, #e2e2e8);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
}
.pc-search-capsule .search-icon {
  font-size: 14px;
  color: var(--text-tertiary, #999999);
  margin-right: 10px;
}
.pc-search-capsule .search-placeholder {
  flex: 1;
  font-size: 14px;
  color: var(--text-secondary, #888888);
}
.pc-search-capsule .shortcut-k {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-tertiary, #aaaaaa);
  background: var(--bg-hover, rgba(0, 0, 0, 0.04));
  padding: 2px 6px;
  border-radius: 6px;
}

/* 顶栏暗色主题适配 (Dark Mode Headers) */
.dark-theme .pc-search-capsule,
[data-theme='dark'] .pc-search-capsule,
.dark .pc-search-capsule {
  background: rgba(255, 255, 255, 0.06) !important;
  border-color: rgba(255, 255, 255, 0.1) !important;
}

.dark-theme .pc-search-capsule:hover,
[data-theme='dark'] .pc-search-capsule:hover,
.dark .pc-search-capsule:hover {
  background: rgba(255, 255, 255, 0.12) !important;
  border-color: rgba(255, 255, 255, 0.18) !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3) !important;
}

.dark-theme .pc-search-capsule .search-icon,
.dark-theme .pc-search-capsule .search-placeholder,
[data-theme='dark'] .pc-search-capsule .search-icon,
[data-theme='dark'] .pc-search-capsule .search-placeholder,
.dark .pc-search-capsule .search-icon,
.dark .pc-search-capsule .search-placeholder {
  color: #94a3b8 !important;
}

.dark-theme .pc-search-capsule .shortcut-k,
[data-theme='dark'] .pc-search-capsule .shortcut-k,
.dark .pc-search-capsule .shortcut-k {
  background: rgba(255, 255, 255, 0.1) !important;
  color: #94a3b8 !important;
}

/* 2. Banner 主卡片 */
.banner-main-card {
  display: flex;
  align-items: center;
  gap: 24px;
}
.banner-cover-wrap {
  width: 104px;
  height: 104px;
  border-radius: 18px;
  overflow: hidden;
  flex-shrink: 0;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.06);
}
.banner-cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.banner-cover-fallback {
  width: 100%;
  height: 100%;
  background: transparent;
  border: 1px solid var(--border-color, #e2e8f0);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-primary, #0f172a);
  font-size: 32px;
  font-weight: 700;
}

.banner-info-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.banner-space-title {
  margin: 0 0 4px 0 !important;
  font-size: 22px !important;
  font-weight: 800 !important;
  color: var(--text-primary, #111111) !important;
  line-height: 1.3 !important;
  display: block !important;
  visibility: visible !important;
  opacity: 1 !important;
  flex-shrink: 0 !important;
  letter-spacing: -0.3px !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
  white-space: nowrap !important;
  max-width: 100% !important;
}

.dark-theme .banner-space-title,
[data-theme='dark'] .banner-space-title,
.dark .banner-space-title {
  color: #ffffff !important;
}

.banner-space-desc {
  margin: 4px 0 10px 0 !important;
  font-size: 13px !important;
  color: var(--text-secondary, #666666) !important;
  line-height: 1.5 !important;
  display: -webkit-box !important;
  -webkit-line-clamp: 2 !important;
  -webkit-box-orient: vertical !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
  word-break: break-word !important;
}

.dark-theme .banner-space-desc,
[data-theme='dark'] .banner-space-desc,
.dark .banner-space-desc {
  color: #94a3b8 !important;
}

.banner-members-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.avatar-stack {
  display: flex;
  align-items: center;
}
.stack-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #ffffff;
  margin-left: -7px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.06);
}
.dark-theme .stack-avatar,
[data-theme='dark'] .stack-avatar,
.dark .stack-avatar {
  border-color: #1e293b !important;
}
.stack-avatar:first-child {
  margin-left: 0;
}
.member-count-text {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary, #666666);
}
.dark-theme .member-count-text,
[data-theme='dark'] .member-count-text,
.dark .member-count-text {
  color: #94a3b8 !important;
}

.private-tag-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.04);
  color: #666666;
  font-size: 12px;
  font-weight: 500;
}
.dark-theme .private-tag-badge,
[data-theme='dark'] .private-tag-badge,
.dark .private-tag-badge {
  background: rgba(255, 255, 255, 0.08) !important;
  color: #cbd5e1 !important;
}
.private-tag-badge .yuemu-icon-wrapper {
  color: #888888;
}
.dark-theme .private-tag-badge .yuemu-icon-wrapper,
[data-theme='dark'] .private-tag-badge .yuemu-icon-wrapper,
.dark .private-tag-badge .yuemu-icon-wrapper {
  color: #94a3b8 !important;
}

.banner-info-body {
  flex: 1 !important;
  min-width: 0 !important;
  margin-right: 16px !important;
  display: flex !important;
  flex-direction: column !important;
  justify-content: center !important;
  overflow: hidden !important;
}

.banner-actions {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.btn-share-icon-only {
  width: 38px;
  height: 38px;
  border-radius: 19px;
  border: 1px solid var(--border-color, rgba(0, 0, 0, 0.12));
  background: var(--card-background, #ffffff);
  color: var(--text-primary, #333333);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.btn-share-icon-only:hover {
  background: #f5f5f7;
  color: #2b7fff;
  border-color: rgba(43, 127, 255, 0.35);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(43, 127, 255, 0.15);
}

.btn-upload-primary {
  height: 40px;
  padding: 0 22px;
  border-radius: 20px;
  border: none;
  background: #2b7fff;
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(43, 127, 255, 0.3);
  transition: all 0.2s ease;
}
.btn-upload-primary:hover {
  background: #1a6bf2;
  box-shadow: 0 6px 18px rgba(43, 127, 255, 0.4);
}

.btn-expand-icon {
  background: transparent;
  border: none;
  color: #aaaaaa;
  font-size: 13px;
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
  transition: color 0.2s ease;
}
.btn-expand-icon:hover {
  color: #333333;
}

/* Sub-Nav 5 大选项卡样式 (Sticky 触顶固定 + 右侧动态搜索) */
.banner-sub-tabs {
  position: sticky !important;
  top: 0 !important;
  z-index: 999 !important;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 32px 14px 32px;
  background: var(--card-background, #ffffff) !important;
  transition: all 0.2s ease;
}

/* 深色主题适配 (Dark Mode Adaptation) */
.dark-theme .banner-sub-tabs,
[data-theme='dark'] .banner-sub-tabs,
.dark .banner-sub-tabs {
  background: rgba(24, 24, 27, 0.88) !important;
  border-top-color: rgba(255, 255, 255, 0.08) !important;
  border-bottom-color: rgba(255, 255, 255, 0.1) !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.5) !important;
}

.dark-theme .banner-sub-tabs .tab-label,
[data-theme='dark'] .banner-sub-tabs .tab-label,
.dark .banner-sub-tabs .tab-label {
  color: #94a3b8 !important;
}

.dark-theme .banner-sub-tabs .tab-item:hover .tab-label,
[data-theme='dark'] .banner-sub-tabs .tab-item:hover .tab-label,
.dark .banner-sub-tabs .tab-item:hover .tab-label {
  color: #e2e8f0 !important;
}

.dark-theme .banner-sub-tabs .tab-item.is-active .tab-label,
[data-theme='dark'] .banner-sub-tabs .tab-item.is-active .tab-label,
.dark .banner-sub-tabs .tab-item.is-active .tab-label {
  color: #ffffff !important;
}

.dark-theme .sticky-right-search,
[data-theme='dark'] .sticky-right-search,
.dark .sticky-right-search {
  background: rgba(255, 255, 255, 0.08) !important;
  border-color: rgba(255, 255, 255, 0.12) !important;
  color: #cbd5e1 !important;
}

.dark-theme .sticky-right-search:hover,
[data-theme='dark'] .sticky-right-search:hover,
.dark .sticky-right-search:hover {
  background: rgba(255, 255, 255, 0.14) !important;
  border-color: rgba(255, 255, 255, 0.2) !important;
}

.dark-theme .sticky-right-search .search-text,
.dark-theme .sticky-right-search .search-icon,
[data-theme='dark'] .sticky-right-search .search-text,
[data-theme='dark'] .sticky-right-search .search-icon,
.dark .sticky-right-search .search-text,
.dark .sticky-right-search .search-icon {
  color: #cbd5e1 !important;
}

.dark-theme .sticky-right-search .shortcut-k,
[data-theme='dark'] .sticky-right-search .shortcut-k,
.dark .sticky-right-search .shortcut-k {
  background: rgba(255, 255, 255, 0.12) !important;
  color: #94a3b8 !important;
}

.banner-sub-tabs.is-sticky-active {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.sub-tabs-left {
  display: flex;
  align-items: center;
  gap: 36px;
}

.sticky-right-search {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--hover-background, #f5f5f7);
  padding: 6px 14px;
  border-radius: 18px;
  cursor: pointer;
  border: 1px solid var(--border-color, #e5e5ea);
  transition: all 0.2s ease;
}
.sticky-right-search:hover {
  background: #ffffff;
  border-color: #2b7fff;
  color: #2b7fff;
  box-shadow: 0 4px 12px rgba(43, 127, 255, 0.15);
}

/* ==========================================================================
   【成员 Tab】1:1 复刻截图极简成员列表样式 (Members Clean List Panel)
   ========================================================================== */
.yuemu-members-tab-panel {
  padding: 24px 20px 48px;
  animation: yuemu-fadeIn 0.35s cubic-bezier(0.16, 1, 0.3, 1);
  max-width: 960px;
  margin: 0 auto;
}

.members-panel-header {
  display: flex !important;
  align-items: center !important;
  justify-content: space-between !important;
  margin-bottom: 20px !important;
  padding-bottom: 12px !important;
  border-bottom: 1px solid var(--border-color, rgba(0, 0, 0, 0.05)) !important;
}

.header-left-title {
  display: flex !important;
  align-items: center !important;
  gap: 8px !important;
}

.header-left-title .title-text {
  font-size: 18px !important;
  font-weight: 800 !important;
  color: var(--text-primary, #111111) !important;
}

.header-left-title .count-num {
  font-size: 16px !important;
  font-weight: 600 !important;
  color: #777777 !important;
}

.header-manage-btn {
  background: rgba(43, 127, 255, 0.08) !important;
  border: 1px solid rgba(43, 127, 255, 0.2) !important;
  color: #2b7fff !important;
  font-size: 13px !important;
  font-weight: 600 !important;
  cursor: pointer !important;
  padding: 5px 14px !important;
  border-radius: 10px !important;
  transition: all 0.2s ease !important;
}

.header-manage-btn:hover {
  background: #2b7fff !important;
  color: #ffffff !important;
}

.members-cards-grid {
  display: grid !important;
  grid-template-columns: repeat(2, 1fr) !important;
  gap: 16px !important;
  width: 100% !important;
  box-sizing: border-box !important;
}

@media (max-width: 640px) {
  .members-cards-grid {
    grid-template-columns: 1fr !important;
  }
}

.member-grid-card {
  position: relative !important;
  display: flex !important;
  align-items: center !important;
  padding: 16px 20px !important;
  background: var(--card-background, #ffffff) !important;
  border: 1px solid var(--border-color, rgba(0, 0, 0, 0.07)) !important;
  border-radius: 20px !important;
  cursor: pointer !important;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1) !important;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.02) !important;
}

.member-grid-card:hover {
  transform: translateY(-4px) !important;
  border-color: rgba(43, 127, 255, 0.35) !important;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.06) !important;
}

.card-avatar-wrap {
  width: 46px !important;
  height: 46px !important;
  border-radius: 50% !important;
  overflow: hidden !important;
  flex-shrink: 0 !important;
  border: 1.5px solid #ffffff !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08) !important;
  margin-right: 14px !important;
}

.card-user-avatar {
  width: 100% !important;
  height: 100% !important;
  object-fit: cover !important;
}

.card-user-info {
  flex: 1 !important;
  min-width: 0 !important;
  display: flex !important;
  flex-direction: column !important;
  gap: 4px !important;
}

.card-name-row {
  display: flex !important;
  align-items: center !important;
  gap: 8px !important;
}

.card-user-name {
  font-size: 15px !important;
  font-weight: 700 !important;
  color: var(--text-primary, #111111) !important;
  margin: 0 !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
  white-space: nowrap !important;
}

.card-role-badge {
  padding: 2px 8px !important;
  border-radius: 8px !important;
  font-size: 11px !important;
  font-weight: 600 !important;
  white-space: nowrap !important;
}

.card-role-badge.is-admin {
  background: rgba(37, 99, 235, 0.1) !important;
  color: #2563eb !important;
}

.card-role-badge.is-member {
  background: rgba(0, 0, 0, 0.05) !important;
  color: #666666 !important;
}

.card-join-time {
  font-size: 12px !important;
  color: #888888 !important;
  margin: 0 !important;
}

.card-more-btn {
  width: 32px !important;
  height: 32px !important;
  border-radius: 50% !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  color: #888888 !important;
  font-size: 14px !important;
  transition: all 0.2s ease !important;
  flex-shrink: 0 !important;
}

.card-more-btn:hover {
  background: rgba(0, 0, 0, 0.06) !important;
  color: #111111 !important;
}

.row-user-box {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar-circle {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

.user-name-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary, #111111);
}

.creator-tag {
  font-size: 14px;
  color: #999999;
  font-weight: 500;
}

.row-role-box {
  display: flex;
  align-items: center;
  gap: 8px;
}

.role-text {
  font-size: 14px;
  font-weight: 600;
  color: #888888;
}

.role-text.is-admin {
  color: #555555;
}

.sticky-right-search .search-icon {
  font-size: 13px;
  color: #888888;
}
.sticky-right-search .search-text {
  font-size: 13px;
  font-weight: 500;
  color: #333333;
}
.sticky-right-search .shortcut-k {
  font-size: 11px;
  font-weight: 600;
  color: #888888;
  background: rgba(0, 0, 0, 0.05);
  padding: 1px 5px;
  border-radius: 4px;
}

.banner-sub-tabs .tab-item {
  position: relative;
  display: flex;
  align-items: center;
  padding: 6px 0 10px 0;
  cursor: pointer;
  user-select: none;
}

.banner-sub-tabs .tab-item.is-disabled {
  opacity: 0.4 !important;
  cursor: not-allowed !important;
  pointer-events: none !important;
}

.banner-sub-tabs .tab-label {
  font-size: 15px;
  font-weight: 500;
  color: #666666;
  transition: all 0.2s ease;
}

.banner-sub-tabs .tab-item:hover .tab-label {
  color: #111111;
}

.banner-sub-tabs .tab-item.is-active .tab-label {
  font-size: 15px;
  font-weight: 700;
  color: #111111;
}

.banner-sub-tabs .active-indicator {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 24px;
  height: 3px;
  background: #2b7fff;
  border-radius: 2px;
}

/* ==========================================================================
   【默认未选择空间 1:1 截图全大封面网格】(Spaces Full Cover Overview Grid)
   ========================================================================== */
.yuemu-my-spaces-overview-hub {
  padding: 32px 36px 96px !important;
  animation: yuemu-fadeIn 0.4s cubic-bezier(0.16, 1, 0.3, 1) !important;
  max-width: 1560px !important;
  margin: 0 auto !important;
  width: 100% !important;
  box-sizing: border-box !important;
  min-height: calc(100vh - 120px) !important;
}

@media (max-width: 768px) {
  .yuemu-my-spaces-overview-hub {
    padding: 16px 14px 96px !important;
  }
  .spaces-full-cover-grid {
    grid-template-columns: 1fr !important;
    gap: 16px !important;
  }
  .space-full-cover-card {
    height: 280px !important;
    border-radius: 20px !important;
  }
}

.spaces-overview-header {
  display: flex !important;
  align-items: center !important;
  justify-content: space-between !important;
  margin-bottom: 24px !important;
}

.overview-title {
  font-size: 24px !important;
  font-weight: 800 !important;
  color: var(--text-primary, #111111) !important;
  margin: 0 !important;
}

.overview-header-actions {
  display: flex !important;
  align-items: center !important;
  gap: 12px !important;
}

.overview-add-space-btn {
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 6px !important;
  height: 34px !important;
  padding: 0 14px !important;
  box-sizing: border-box !important;
  background: #2563eb !important;
  color: #ffffff !important;
  border: none !important;
  border-radius: 10px !important;
  font-size: 13px !important;
  font-weight: 600 !important;
  cursor: pointer !important;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1) !important;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2) !important;
}

.overview-add-space-btn:hover {
  background: #1d4ed8 !important;
  transform: translateY(-1px) !important;
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.3) !important;
}

.overview-add-space-btn:active {
  transform: translateY(0) !important;
}

.view-switch-btns {
  display: flex !important;
  align-items: center !important;
  gap: 6px !important;
}

.switch-icon-btn {
  width: 34px !important;
  height: 34px !important;
  border-radius: 10px !important;
  border: 1px solid var(--border-color, rgba(0, 0, 0, 0.08)) !important;
  background: var(--card-background, #ffffff) !important;
  color: #777777 !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  cursor: pointer !important;
  transition: all 0.2s ease !important;
}

.switch-icon-btn.active, .switch-icon-btn:hover {
  border-color: #2b7fff !important;
  color: #2b7fff !important;
  background: rgba(43, 127, 255, 0.06) !important;
}

.spaces-full-cover-grid {
  display: grid !important;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)) !important;
  gap: 24px !important;
  width: 100% !important;
  box-sizing: border-box !important;
}

.space-full-cover-card {
  position: relative !important;
  height: 320px !important;
  border-radius: 24px !important;
  overflow: hidden !important;
  cursor: pointer !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06) !important;
  transition: all 0.35s cubic-bezier(0.16, 1, 0.3, 1) !important;
  background: #f1f5f9 !important;
  box-sizing: border-box !important;
}

.space-full-cover-card:hover {
  transform: translateY(-6px) !important;
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.16) !important;
}

.card-bg-cover {
  width: 100% !important;
  height: 100% !important;
  max-height: 320px !important;
  object-fit: cover !important;
  display: block !important;
  transition: transform 0.5s ease !important;
}

.space-full-cover-card:hover .card-bg-cover {
  transform: scale(1.06) !important;
}

.card-type-tag {
  position: absolute !important;
  top: 14px !important;
  right: 14px !important;
  padding: 4px 12px !important;
  border-radius: 12px !important;
  font-size: 11px !important;
  font-weight: 700 !important;
  color: #ffffff !important;
  backdrop-filter: blur(10px) !important;
  z-index: 3 !important;
}
.card-type-tag.is-private { background: rgba(37, 99, 235, 0.85) !important; }
.card-type-tag.is-team { background: rgba(16, 185, 129, 0.85) !important; }

.card-bottom-overlay {
  position: absolute !important;
  inset: 0 !important;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.88) 0%, rgba(0, 0, 0, 0.45) 45%, transparent 75%) !important;
  display: flex !important;
  flex-direction: column !important;
  justify-content: flex-end !important;
  padding: 24px !important;
  z-index: 2 !important;
  box-sizing: border-box !important;
}

.card-title-white {
  font-size: 20px !important;
  font-weight: 800 !important;
  color: #ffffff !important;
  margin: 0 0 6px 0 !important;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3) !important;
}

.card-desc-white {
  font-size: 13px !important;
  color: rgba(255, 255, 255, 0.8) !important;
  margin: 0 0 14px 0 !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
  white-space: nowrap !important;
}

.card-meta-white {
  display: flex !important;
  align-items: center !important;
  gap: 12px !important;
}

.avatar-stack-wrap {
  display: flex !important;
  align-items: center !important;
}

.stack-avatar-mini {
  width: 24px !important;
  height: 24px !important;
  border-radius: 50% !important;
  border: 1.5px solid #ffffff !important;
  margin-right: -8px !important;
  object-fit: cover !important;
}

.meta-stats-text {
  font-size: 12px !important;
  color: rgba(255, 255, 255, 0.85) !important;
  font-weight: 500 !important;
}

/* 列表模式：大屏一行 2 个双列平铺 */
.spaces-list-view-wrap {
  display: grid !important;
  grid-template-columns: repeat(2, 1fr) !important;
  gap: 16px !important;
  width: 100% !important;
  box-sizing: border-box !important;
}

@media (max-width: 768px) {
  .spaces-list-view-wrap {
    grid-template-columns: 1fr !important;
  }
}

.space-list-row-item {
  display: flex !important;
  align-items: center !important;
  justify-content: space-between !important;
  padding: 16px 20px !important;
  background: var(--card-background, #ffffff) !important;
  border: 1px solid var(--border-color, rgba(0, 0, 0, 0.06)) !important;
  border-radius: 18px !important;
  cursor: pointer !important;
  transition: all 0.25s ease !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02) !important;
}

.space-list-row-item:hover {
  transform: translateX(4px) !important;
  border-color: rgba(43, 127, 255, 0.4) !important;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.06) !important;
}

.row-cover-box {
  width: 80px !important;
  height: 60px !important;
  border-radius: 12px !important;
  overflow: hidden !important;
  flex-shrink: 0 !important;
}

.row-cover-img {
  width: 100% !important;
  height: 100% !important;
  object-fit: cover !important;
}

.row-info-main {
  flex: 1 !important;
  min-width: 0 !important;
  margin: 0 14px !important;
  display: flex !important;
  flex-direction: column !important;
  gap: 4px !important;
}

.row-name-bar {
  display: flex !important;
  align-items: center !important;
  gap: 8px !important;
  min-width: 0 !important;
}

.row-space-name {
  font-size: 15px !important;
  font-weight: 700 !important;
  color: var(--text-primary, #111111) !important;
  margin: 0 !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
  white-space: nowrap !important;
}

.row-type-badge {
  padding: 2px 8px !important;
  border-radius: 8px !important;
  font-size: 11px !important;
  font-weight: 600 !important;
  flex-shrink: 0 !important;
}
.row-type-badge.is-private { background: rgba(37, 99, 235, 0.1) !important; color: #2563eb !important; }
.row-type-badge.is-team { background: rgba(16, 185, 129, 0.1) !important; color: #10b981 !important; }

.row-space-desc {
  font-size: 12px !important;
  color: #777777 !important;
  margin: 0 !important;
  display: -webkit-box !important;
  -webkit-line-clamp: 1 !important;
  -webkit-box-orient: vertical !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
  white-space: normal !important;
}

.row-meta-right {
  display: flex !important;
  align-items: center !important;
  gap: 12px !important;
  flex-shrink: 0 !important;
}

.row-stats-text {
  font-size: 12px !important;
  color: #888888 !important;
}

@media (max-width: 768px) {
  .space-list-row-item {
    padding: 12px 14px !important;
  }
  .row-info-main {
    margin: 0 10px !important;
  }
  .row-stats-text {
    display: none !important;
  }
}

.row-enter-btn {
  width: 32px !important;
  height: 32px !important;
  border-radius: 50% !important;
  background: rgba(43, 127, 255, 0.08) !important;
  color: #2b7fff !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  font-size: 13px !important;
  transition: all 0.2s ease !important;
}

.space-list-row-item:hover .row-enter-btn {
  background: #2b7fff !important;
  color: #ffffff !important;
  transform: translateX(3px) !important;
}

/* 空空间居中大卡片 (上下左右绝对居中) */
.welcome-empty-spaces {
  text-align: center !important;
  padding: 80px 32px !important;
  background: var(--card-background, #ffffff) !important;
  border-radius: 24px !important;
  border: 1px dashed var(--border-color, rgba(0, 0, 0, 0.12)) !important;
  margin: 60px auto !important;
  max-width: 520px !important;
  width: 100% !important;
  display: flex !important;
  flex-direction: column !important;
  align-items: center !important;
  justify-content: center !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.02) !important;
}

.welcome-empty-spaces .empty-sparkle-box {
  width: 64px !important;
  height: 64px !important;
  border-radius: 20px !important;
  background: rgba(43, 127, 255, 0.08) !important;
  color: #2b7fff !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  font-size: 28px !important;
  margin: 0 auto 20px !important;
}

.welcome-empty-spaces h4 {
  font-size: 18px !important;
  font-weight: 700 !important;
  color: var(--text-primary, #111111) !important;
  margin: 0 0 8px 0 !important;
}

.welcome-empty-spaces p {
  font-size: 14px !important;
  color: #888888 !important;
  margin: 0 !important;
}

.pending-icon-box {
  width: 64px;
  height: 64px;
  border-radius: 20px;
  background: #eff6ff;
  color: #2b7fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  margin-bottom: 20px;
}

.pending-title {
  font-size: 20px;
  font-weight: 700;
  color: #111111;
  margin: 0 0 8px 0;
}

.pending-subtitle {
  font-size: 14px;
  color: #666666;
  max-width: 400px;
  margin: 0 0 24px 0;
  line-height: 1.5;
}

.pending-back-btn {
  height: 38px;
  padding: 0 20px;
  border-radius: 19px;
  border: none;
  background: #2b7fff;
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}
.pending-back-btn:hover {
  background: #1a6bf2;
}

/* ==========================================================================
   【设置 Tab】极简纯粹功能选项行 (点击分析跳转，点击编辑开弹窗)
   ========================================================================== */
.yuemu-settings-pure-wrap {
  padding: 28px 20px 48px;
  animation: yuemu-fadeIn 0.35s cubic-bezier(0.16, 1, 0.3, 1);
  max-width: 600px;
  margin: 0 auto;
}

.settings-options-card {
  background: var(--card-background, #ffffff);
  border: 1px solid var(--border-color, rgba(0, 0, 0, 0.08));
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.02);
}

.settings-option-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 22px 28px;
  cursor: pointer;
  transition: all 0.25s ease;
}

.settings-option-item:hover {
  background-color: var(--hover-background, #f8f9fa);
}

.option-left {
  display: flex;
  align-items: center;
  gap: 18px;
}

.option-icon-box {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #ffffff;
  flex-shrink: 0;
}
.option-icon-box.bg-blue {
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
}
.option-icon-box.bg-green {
  background: linear-gradient(135deg, #10b981, #059669);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.25);
}
.option-icon-box.bg-purple {
  background: linear-gradient(135deg, #8b5cf6, #6366f1);
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.25);
}

.option-text-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.option-title {
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary, #111111);
  margin: 0;
}

.option-desc {
  font-size: 13px;
  color: #777777;
  margin: 0;
}

.option-right .arrow-icon {
  font-size: 16px;
  color: #bbbbbb;
  transition: transform 0.25s ease, color 0.25s ease;
}

.settings-option-item:hover .option-right .arrow-icon {
  color: #2b7fff;
  transform: translateX(4px);
}

.option-divider {
  height: 1px;
  background-color: var(--border-color, rgba(0, 0, 0, 0.06));
  margin: 0 28px;
}

/* ==========================================================================
   Apple Today / App Store 级活动网格卡片 (Apple Activity Card Grid)
   ========================================================================== */
.yuemu-apple-activities-wrap {
  padding: 28px 32px 48px;
  animation: yuemu-fadeIn 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}

.activities-loading-state {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 80px 0;
}

.apple-activity-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 28px;
}

.apple-activity-card {
  display: flex;
  flex-direction: column;
  background: var(--card-background, #ffffff);
  border: 1px solid var(--border-color, rgba(0, 0, 0, 0.08));
  border-radius: 24px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.apple-activity-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08);
  border-color: rgba(43, 127, 255, 0.35);
}

/* 1. 16:9 大图封面 */
.card-cover-wrapper {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: #f5f5f7;
}

.card-cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.apple-activity-card:hover .card-cover-img {
  transform: scale(1.06);
}

/* Apple 极简 Badge */
.card-status-tag {
  position: absolute;
  top: 14px;
  left: 14px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(12px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}

.card-status-tag.is-live .status-dot {
  background: #10b981;
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.6);
}

.card-status-tag.is-expired .status-dot {
  background: #9ca3af;
}

.status-text {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.3px;
}
.card-status-tag.is-live .status-text { color: #047857; }
.card-status-tag.is-expired .status-text { color: #4b5563; }

/* 2. 卡片主体与 Typography 层次 */
.card-content-body {
  padding: 22px 24px 20px;
  display: flex;
  flex-direction: column;
  flex: 1;
  justify-content: space-between;
}

.card-header-info {
  margin-bottom: 14px;
}

.card-title {
  font-size: 22px;
  font-weight: 800;
  color: var(--text-primary, #111111);
  margin: 0 0 6px 0;
  line-height: 1.25;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-subtitle {
  font-size: 14px;
  color: var(--text-secondary, #666666);
  margin: 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 3. 用户视角 Meta 行 */
.card-meta-pills {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 18px;
}

.meta-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 10px;
  background: var(--hover-background, #f5f5f7);
  font-size: 12px;
  font-weight: 600;
  color: #555555;
}
.meta-pill .yuemu-icon-wrapper {
  color: #2b7fff;
}

/* 4. 底部分割线与 CTA 按钮 */
.card-action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 14px;
  border-top: 1px solid var(--border-color, rgba(0, 0, 0, 0.06));
}

.author-info {
  display: flex;
  align-items: center;
  gap: 4px;
}

.author-label {
  font-size: 11px;
  color: #999999;
}

.author-name {
  font-size: 12px;
  font-weight: 600;
  color: #444444;
}

.cta-join-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 16px;
  border-radius: 17px;
  border: none;
  background: #2b7fff;
  color: #ffffff;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(43, 127, 255, 0.25);
  transition: all 0.2s ease;
}

.apple-activity-card:hover .cta-join-btn {
  background: #1a6bf2;
  box-shadow: 0 6px 16px rgba(43, 127, 255, 0.35);
  transform: translateX(2px);
}
.cta-join-btn.disabled {
  opacity: 0.4;
  cursor: not-allowed;
  pointer-events: none;
}

/* 空状态 */
.apple-empty-activities {
  padding: 100px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.empty-sparkle-box {
  width: 64px;
  height: 64px;
  border-radius: 20px;
  background: #eff6ff;
  color: #2b7fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-bottom: 16px;
}

.apple-empty-activities .empty-title {
  font-size: 18px;
  font-weight: 800;
  color: #111111;
  margin: 0 0 6px 0;
}

.pictures-tab-pure-wrap {
  position: relative !important;
  width: 100% !important;
}

/* 真正脱离列表高度的顶部悬空指示器胶囊 */
.floating-refresh-pill {
  position: absolute !important;
  top: -46px !important;
  left: 50% !important;
  z-index: 1000 !important;
  height: 36px !important;
  padding: 0 18px !important;
  border-radius: 18px !important;
  background: rgba(255, 255, 255, 0.95) !important;
  backdrop-filter: blur(12px) !important;
  -webkit-backdrop-filter: blur(12px) !important;
  border: 1px solid var(--border-color, rgba(0, 0, 0, 0.08)) !important;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12) !important;
  color: #2b7fff !important;
  font-size: 13px !important;
  font-weight: 600 !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 8px !important;
  pointer-events: none !important;
  transition: opacity 0.2s ease !important;
}

.floating-refresh-pill .pull-arrow {
  display: inline-block !important;
  transition: transform 0.2s ease !important;
}

.apple-empty-activities .empty-desc {
  font-size: 14px;
  color: #888888;
  margin: 0;
}

/* 移动端 (<=768px) 高保真响应式美化与横滑 */
@media (max-width: 768px) {
  .mobile-space-immersive-banner {
    position: relative !important;
    width: 100% !important;
    min-height: 140px !important;
    flex-shrink: 0 !important;
    box-sizing: border-box !important;
    padding-bottom: 24px !important;
  }

  .banner-sub-tabs {
    position: sticky !important;
    top: 0 !important;
    z-index: 999 !important;
    overflow-x: auto !important;
    white-space: nowrap !important;
    scrollbar-width: none !important;
    padding: 18px 20px 12px 20px !important;
    margin-top: 0 !important;
    border-radius: 12px 12px 0 0 !important;
    background: var(--card-background, #ffffff) !important;
    backdrop-filter: blur(16px) !important;
    -webkit-backdrop-filter: blur(16px) !important;
    border-bottom: 1px solid var(--border-color, rgba(0, 0, 0, 0.06)) !important;
    box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.05) !important;
    transition: border-radius 0.2s ease, margin-top 0.2s ease, box-shadow 0.2s ease !important;
  }

  .banner-sub-tabs.is-sticky-active {
    border-radius: 0 !important;
    margin-top: 0 !important;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06) !important;
  }

  .banner-sub-tabs::-webkit-scrollbar {
    display: none !important;
  }
  .sub-tabs-left {
    gap: 28px !important;
  }
  .banner-sub-tabs .tab-label {
    font-size: 15px !important;
    font-weight: 600 !important;
  }

  .yuemu-apple-activities-wrap,
  .pictures-tab-pure-wrap,
  .yuemu-members-tab-panel {
    padding: 16px 14px 32px !important;
    box-sizing: border-box !important;
  }
  .apple-activity-grid {
    grid-template-columns: 1fr !important;
    gap: 20px !important;
  }
  .card-content-body {
    padding: 16px 18px !important;
  }
  .card-title {
    font-size: 19px !important;
  }
}

/* ==========================================================================
   【移动端三栏拆分导航与全屏视口】(Mobile 3-Column Split Nav & Full View)
   ========================================================================== */
.mobile-space-bottom-nav {
  position: fixed !important;
  bottom: 0 !important;
  left: 0 !important;
  right: 0 !important;
  height: 56px !important;
  background: rgba(255, 255, 255, 0.94) !important;
  backdrop-filter: blur(16px) !important;
  -webkit-backdrop-filter: blur(16px) !important;
  border-top: 1px solid var(--border-color, rgba(0, 0, 0, 0.08)) !important;
  display: flex !important;
  align-items: center !important;
  justify-content: space-around !important;
  z-index: 9999 !important;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.05) !important;
  padding-bottom: env(safe-area-inset-bottom, 0px) !important;
}

.mobile-space-bottom-nav .nav-tab-item {
  display: flex !important;
  flex-direction: column !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 3px !important;
  flex: 1 !important;
  height: 100% !important;
  color: #777777 !important;
  cursor: pointer !important;
  transition: all 0.2s ease !important;
}

.mobile-space-bottom-nav .nav-tab-item.is-active {
  color: #2b7fff !important;
}

.mobile-space-bottom-nav .nav-tab-item .tab-icon {
  font-size: 18px !important;
}

.mobile-space-bottom-nav .nav-tab-item .tab-label {
  font-size: 11px !important;
  font-weight: 600 !important;
}

/* 移动端三栏全屏同构视口 */
@media (max-width: 768px) {
  .yuemu-space-left-sider {
    width: 100vw !important;
    height: calc(100vh - 56px) !important;
    position: fixed !important;
    top: 0 !important;
    left: 0 !important;
    margin: 0 !important;
    border-radius: 0 !important;
    z-index: 9988 !important;
    border: none !important;
    padding-bottom: 56px !important;
    background: var(--card-background, #ffffff) !important;
  }
}

.pictures-tab-pure-wrap {
  position: relative !important;
  width: 100% !important;
  margin: 0 !important;
  padding: 0 !important;
}

.pictures-touch-layer {
  position: relative !important;
  width: 100% !important;
  margin: 0 !important;
  padding: 0 !important;
  transform: none !important;
}

/* 沉浸顶栏下拉刷新指示器 (包含箭头/Spinner + 状态文案，绝对定位零高度抖动) */
.mypage-style-top-spinner {
  position: absolute !important;
  left: 50% !important;
  top: 50% !important;
  transform: translate(-50%, -50%) !important;
  color: #ffffff !important;
  font-size: 13px !important;
  font-weight: 500 !important;
  pointer-events: none !important;
  z-index: 10 !important;
  display: flex !important;
  align-items: center !important;
  gap: 6px !important;
  background: rgba(0, 0, 0, 0.45) !important;
  padding: 4px 12px !important;
  border-radius: 20px !important;
  backdrop-filter: blur(8px) !important;
  -webkit-backdrop-filter: blur(8px) !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15) !important;
  transition: all 0.2s ease !important;
}

.mypage-style-top-spinner .refresh-arrow-icon {
  font-size: 12px !important;
  transition: transform 0.2s ease !important;
}

.mypage-style-top-spinner .refresh-tip-text {
  font-size: 12px !important;
  white-space: nowrap !important;
  letter-spacing: 0.5px !important;
}

.mobile-full-chat-view {
  position: fixed !important;
  top: 0 !important;
  left: 0 !important;
  right: 0 !important;
  bottom: 56px !important;
  width: 100vw !important;
  height: calc(100vh - 56px) !important;
  z-index: 9988 !important;
  background: #ffffff !important;
  border-radius: 0 !important;
  margin: 0 !important;
  padding: 0 !important;
}

/* ==========================================================================
   【移动端专属电影级沉浸 Banner Header】(100% 像素级复刻参考截图)
   ========================================================================== */
.mobile-space-immersive-banner {
  position: relative !important;
  width: 100% !important;
  padding: 16px 20px 24px 20px !important;
  box-sizing: border-box !important;
  overflow: hidden !important;
  color: #ffffff !important;
  background: #111827 !important;
  min-height: 220px !important;
  display: flex !important;
  flex-direction: column !important;
  justify-content: space-between !important;
}

.mobile-space-immersive-banner .banner-bg-img {
  position: absolute !important;
  inset: 0 !important;
  width: 100% !important;
  height: 100% !important;
  object-fit: cover !important;
  /* 保持封面大图自然高亮色彩与清爽清晰度 */
  filter: brightness(0.92) !important;
  transform: scale(1.02) !important;
  z-index: 1 !important;
}

.mobile-space-immersive-banner .banner-mask-overlay {
  position: absolute !important;
  inset: 0 !important;
  /* 左黑蓝暗向右渐变过渡到全透，保证左字清晰且右图（荷花）全亮展示 */
  background: linear-gradient(to right, rgba(15, 23, 42, 0.88) 0%, rgba(15, 23, 42, 0.55) 50%, rgba(0, 0, 0, 0.1) 100%),
              linear-gradient(to bottom, rgba(0, 0, 0, 0.35) 0%, transparent 40%, rgba(0, 0, 0, 0.65) 100%) !important;
  z-index: 2 !important;
}

.mobile-top-bar {
  position: relative !important;
  z-index: 3 !important;
  display: flex !important;
  align-items: center !important;
  justify-content: space-between !important;
  margin-bottom: 24px !important;
}

.mobile-back-icon, .mobile-icon-btn {
  width: 32px !important;
  height: 32px !important;
  border-radius: 0 !important;
  background: transparent !important;
  backdrop-filter: none !important;
  border: none !important;
  color: #ffffff !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  font-size: 20px !important;
  cursor: pointer !important;
  filter: drop-shadow(0 1px 4px rgba(0, 0, 0, 0.6)) !important;
  transition: opacity 0.2s ease !important;
}

.mobile-back-icon:active, .mobile-icon-btn:active {
  opacity: 0.7 !important;
}

.mobile-top-right-actions {
  display: flex !important;
  align-items: center !important;
  gap: 16px !important;
}

.mobile-banner-content {
  position: relative !important;
  z-index: 3 !important;
  display: flex !important;
  flex-direction: column !important;
  gap: 6px !important;
}

.mobile-space-title {
  font-size: 24px !important;
  font-weight: 700 !important;
  color: #ffffff !important;
  margin: 0 !important;
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.6) !important;
}

.mobile-online-tag {
  display: inline-flex !important;
  align-items: center !important;
  gap: 6px !important;
  font-size: 13px !important;
  color: rgba(255, 255, 255, 0.9) !important;
}

.mobile-online-tag .green-dot {
  width: 8px !important;
  height: 8px !important;
  border-radius: 50% !important;
  background: #10b981 !important;
  box-shadow: 0 0 8px #10b981 !important;
}

.mobile-space-desc {
  font-size: 13px !important;
  color: rgba(255, 255, 255, 0.8) !important;
  margin: 2px 0 6px 0 !important;
  line-height: 1.4 !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
  display: -webkit-box !important;
  -webkit-line-clamp: 2 !important;
  -webkit-box-orient: vertical !important;
}

.mobile-members-avatar-row {
  display: flex !important;
  align-items: center !important;
  gap: 10px !important;
}

.mobile-avatar-stack {
  display: flex !important;
  align-items: center !important;
}

.mobile-stack-avatar {
  width: 26px !important;
  height: 26px !important;
  border-radius: 50% !important;
  object-fit: cover !important;
  border: 1.5px solid #ffffff !important;
  margin-left: -7px !important;
}
.mobile-stack-avatar:first-child {
  margin-left: 0 !important;
}

.mobile-member-count {
  font-size: 12px !important;
  color: rgba(255, 255, 255, 0.85) !important;
  font-weight: 500 !important;
}

/* 移动端 Header 标题可点击下拉按钮 (摄影小分队 ▼) */
.mobile-space-title-btn {
  display: inline-flex !important;
  align-items: center !important;
  cursor: pointer !important;
  user-select: none !important;
  border-radius: 8px !important;
  padding: 2px 6px 2px 0 !important;
  transition: opacity 0.2s ease !important;
}

.mobile-space-title-btn:active {
  opacity: 0.7 !important;
}

.mobile-space-title {
  display: flex !important;
  align-items: center !important;
  gap: 6px !important;
}

.mobile-space-title .title-arrow-icon {
  font-size: 14px !important;
  color: rgba(255, 255, 255, 0.85) !important;
  transition: transform 0.2s ease !important;
}

/* 移动端 Apple/微信/飞书 风格 【切换空间】抽屉 (直接 1:1 嵌入完整的 SpaceDetailLeftSider 侧边栏组件) */
/* 移动端 Apple/微信/飞书 风格 【切换空间】抽屉 */
/* 切换空间抽屉内胶囊新建空间按钮高奢样式 */
.pill-create-space-btn {
  width: 100%;
  padding: 12px 16px;
  border-radius: 14px;
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.25);
  transition: all 0.2s ease;
}

.pill-create-space-btn:active {
  transform: scale(0.98);
}

/* 自定义原生 Vue div 弹窗 (全量支持 Dark Mode 适配与优雅居中) */
.apple-overlay {
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  padding: 20px !important;
}

.yuemu-custom-modal {
  width: 520px;
  max-width: 92vw;
  background: var(--card-background, #ffffff);
  border: 1px solid var(--border-color, #e2e8f0);
  border-radius: 24px;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.22);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: yuemuPop 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  color: var(--text-primary, #0f172a);
  margin: auto;
}

.yuemu-custom-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px 14px;
  border-bottom: 1px solid var(--border-color, rgba(226, 232, 240, 0.6));
}

.rank-icon-badge {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.15) 0%, rgba(217, 119, 6, 0.25) 100%);
  color: #f59e0b;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.rank-title-group {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.yuemu-custom-modal-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary, #0f172a);
}

.yuemu-custom-modal-close {
  background: var(--hover-background, #f1f5f9);
  border: none;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  color: var(--text-secondary, #64748b);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s ease;
}

.yuemu-custom-modal-close:hover {
  background: var(--border-color, #cbd5e1);
  color: var(--text-primary, #0f172a);
}

.yuemu-custom-modal-body {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.yuemu-custom-textarea {
  width: 100%;
  background: var(--hover-background, #f8fafc);
  border: 1px solid var(--border-color, #e2e8f0);
  border-radius: 14px;
  padding: 12px 16px;
  color: var(--text-primary, #0f172a);
  font-size: 14px;
  outline: none;
  resize: vertical;
  box-sizing: border-box;
}

.yuemu-custom-textarea:focus {
  border-color: var(--link-color, #2563eb);
}

.yuemu-custom-modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 14px 24px 20px;
  border-top: 1px solid var(--border-color, #f1f5f9);
}

.yuemu-modal-btn {
  padding: 8px 20px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 6px;
}

.yuemu-modal-btn.cancel {
  background: var(--hover-background, #f1f5f9);
  color: var(--text-secondary, #64748b);
  border: 1px solid var(--border-color, #cbd5e1);
}

.yuemu-modal-btn.confirm {
  background: var(--link-color, #2563eb);
  color: #ffffff;
}

.yuemu-modal-btn.confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.mobile-space-sheet-overlay {
  z-index: 30000 !important;
}

.mobile-space-sheet {
  background: var(--card-background, #ffffff) !important;
  border-radius: 24px 24px 0 0 !important;
  padding: 16px 20px 28px !important;
  box-shadow: 0 -10px 40px rgba(0, 0, 0, 0.18) !important;
  max-height: 85vh !important;
  display: flex !important;
  flex-direction: column !important;
  z-index: 30001 !important;
}

.mobile-space-sheet .sheet-header {
  display: flex !important;
  align-items: center !important;
  justify-content: space-between !important;
  padding: 4px 8px 16px !important;
  margin-bottom: 12px !important;
  border-bottom: 1px solid var(--border-color, rgba(0, 0, 0, 0.06)) !important;
}

.mobile-space-sheet .sheet-title {
  font-size: 17px !important;
  font-weight: 700 !important;
  color: var(--text-primary, #1d1d1f) !important;
  margin: 0 !important;
}

.mobile-space-sheet .sheet-close-btn {
  background: rgba(0, 0, 0, 0.05) !important;
  border: none !important;
  width: 28px !important;
  height: 28px !important;
  border-radius: 50% !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  color: var(--text-secondary, #86868b) !important;
  cursor: pointer !important;
}

.mobile-space-sheet .sheet-sider-body {
  flex: 1 !important;
  overflow-y: auto !important;
  -webkit-overflow-scrolling: touch !important;
  touch-action: pan-y !important;
  padding: 12px 4px !important;
  display: flex !important;
  flex-direction: column !important;
  gap: 12px !important;
  max-height: 65vh !important;
}

.mobile-space-sheet .create-space-card {
  border: 1px solid rgba(37, 99, 235, 0.25) !important;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.08) 0%, rgba(59, 130, 246, 0.14) 100%) !important;
  margin-bottom: 6px !important;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.1) !important;
  transition: all 0.2s ease !important;
}

.mobile-space-sheet .create-space-card:active {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.15) 0%, rgba(59, 130, 246, 0.22) 100%) !important;
  transform: scale(0.98) !important;
}

.mobile-space-sheet .create-avatar-box {
  background: var(--link-color, #2563eb) !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  color: #ffffff !important;
  font-size: 18px !important;
  box-shadow: 0 3px 10px rgba(37, 99, 235, 0.35) !important;
  border-radius: 12px !important;
}

.mobile-space-sheet .create-title {
  color: var(--link-color, #2563eb) !important;
  font-weight: 700 !important;
  font-size: 15px !important;
}

.mobile-space-sheet .create-badge {
  font-size: 11px !important;
  padding: 2px 10px !important;
  border-radius: 99px !important;
  background: var(--link-color, #2563eb) !important;
  color: #ffffff !important;
  font-weight: 600 !important;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.25) !important;
}

.mobile-space-sheet .sheet-create-btn-wrap {
  margin-bottom: 14px !important;
}

.mobile-space-sheet .spaces-list {
  display: flex !important;
  flex-direction: column !important;
  gap: 12px !important;
  overflow-y: auto !important;
  -webkit-overflow-scrolling: touch !important;
  touch-action: pan-y !important;
  max-height: 55vh !important;
  padding-right: 2px !important;
}

.mobile-space-sheet .space-card-item {
  display: flex !important;
  align-items: center !important;
  gap: 14px !important;
  padding: 12px 16px !important;
  border-radius: 16px !important;
  background: var(--card-background, #ffffff) !important;
  border: 1px solid var(--border-color, rgba(0, 0, 0, 0.06)) !important;
  cursor: pointer !important;
  transition: all 0.2s ease !important;
  margin-bottom: 2px !important;
}

.mobile-space-sheet .space-card-item:active {
  transform: scale(0.98) !important;
}

.mobile-space-sheet .space-card-item.is-active {
  background: rgba(37, 99, 235, 0.08) !important;
  border-color: rgba(37, 99, 235, 0.3) !important;
}

.mobile-space-sheet .space-avatar-wrap {
  width: 44px !important;
  height: 44px !important;
  border-radius: 12px !important;
  overflow: hidden !important;
  flex-shrink: 0 !important;
}

.mobile-space-sheet .space-avatar-img {
  width: 100% !important;
  height: 100% !important;
  object-fit: cover !important;
}

.mobile-space-sheet .space-avatar-fallback {
  width: 100% !important;
  height: 100% !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  color: #ffffff !important;
  font-weight: 700 !important;
  font-size: 16px !important;
}

.mobile-space-sheet .space-info {
  flex: 1 !important;
  min-width: 0 !important;
  display: flex !important;
  flex-direction: column !important;
  gap: 2px !important;
}

.mobile-space-sheet .space-name-row {
  display: flex !important;
  align-items: center !important;
  justify-content: space-between !important;
  gap: 8px !important;
}

.mobile-space-sheet .space-name {
  font-size: 15px !important;
  font-weight: 600 !important;
  color: var(--text-primary, #1e293b) !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
  white-space: nowrap !important;
}

.mobile-space-sheet .space-card-item.is-active .space-name {
  color: var(--link-color, #2563eb) !important;
}

.mobile-space-sheet .space-type-tag {
  font-size: 11px !important;
  padding: 2px 8px !important;
  border-radius: 8px !important;
  font-weight: 500 !important;
  flex-shrink: 0 !important;
}

.mobile-space-sheet .space-type-tag.is-private {
  background: rgba(37, 99, 235, 0.1) !important;
  color: #2563eb !important;
}

.mobile-space-sheet .space-type-tag.is-team {
  background: rgba(16, 185, 129, 0.1) !important;
  color: #10b981 !important;
}

.mobile-space-sheet .spaces-loading-more-tip,
.mobile-space-sheet .spaces-no-more-tip,
.space-list-loading-more,
.space-list-no-more {
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 8px !important;
  padding: 20px 0 30px !important;
  font-size: 13px !important;
  color: var(--text-tertiary, #94a3b8) !important;
  width: 100% !important;
  text-align: center !important;
}

/* 移动端吸顶搜索胶囊：仅保留 🔍 放大镜搜索图标 */
@media (max-width: 768px) {
  .banner-sub-tabs .sticky-right-search {
    padding: 6px 12px !important;
    border-radius: 99px !important;
    min-width: unset !important;
    background: var(--hover-background, rgba(0, 0, 0, 0.06)) !important;
    border: 1px solid var(--border-color, rgba(0, 0, 0, 0.1)) !important;
    color: var(--text-primary) !important;
  }

  html.dark .banner-sub-tabs .sticky-right-search,
  body.dark .banner-sub-tabs .sticky-right-search,
  .dark-theme .banner-sub-tabs .sticky-right-search,
  [data-theme='dark'] .banner-sub-tabs .sticky-right-search,
  .dark .banner-sub-tabs .sticky-right-search {
    background: rgba(255, 255, 255, 0.16) !important;
    border-color: rgba(255, 255, 255, 0.24) !important;
    color: #ffffff !important;
  }

  html.dark .banner-sub-tabs .sticky-right-search .search-icon,
  body.dark .banner-sub-tabs .sticky-right-search .search-icon,
  .dark-theme .banner-sub-tabs .sticky-right-search .search-icon,
  [data-theme='dark'] .banner-sub-tabs .sticky-right-search .search-icon,
  .dark .banner-sub-tabs .sticky-right-search .search-icon {
    color: #ffffff !important;
  }

  .banner-sub-tabs .sticky-right-search .search-text,
  .banner-sub-tabs .sticky-right-search .shortcut-k {
    display: none !important;
  }
}

  /* 悬浮上传按钮 (.fab-radial-container) 位于 52px 聊天胶囊卡片正上方 */
  .fab-radial-container {
    bottom: calc(72px + env(safe-area-inset-bottom)) !important;
    right: 20px !important;
    z-index: 9980 !important;
    transition: all 0.3s ease !important;
  }

/* 移动端 Apple 风格 1:1 悬浮胶囊聊天抽屉 (高保真截图设计) */
.mobile-chat-apple-sheet {
  position: fixed !important;
  left: 12px !important;
  right: 12px !important;
  bottom: calc(10px + env(safe-area-inset-bottom)) !important;
  z-index: 9990 !important;
  background: var(--card-background, #ffffff) !important;
  border-radius: 28px !important;
  box-shadow: 0 10px 36px rgba(0, 0, 0, 0.14), 0 2px 10px rgba(0, 0, 0, 0.06) !important;
  border: 1px solid var(--border-color, rgba(0, 0, 0, 0.08)) !important;
  transition: all 0.32s cubic-bezier(0.16, 1, 0.3, 1) !important;
  display: flex !important;
  flex-direction: column !important;
  overflow: hidden !important;
}

/* 折叠态：悬浮冰蓝胶囊 52px */
.mobile-chat-apple-sheet.is-collapsed {
  height: 52px !important;
  background: rgba(244, 247, 251, 0.96) !important;
  backdrop-filter: blur(12px) !important;
}

/* 展开态：直接 85vh 大抽屉 */
.mobile-chat-apple-sheet.is-expanded {
  left: 0 !important;
  right: 0 !important;
  bottom: 0 !important;
  border-radius: 24px 24px 0 0 !important;
  height: 85vh !important;
  background: var(--card-background, #ffffff) !important;
}

/* 1:1 Handle 抓手胶囊栏 */
.mobile-chat-apple-sheet .sheet-handle-capsule-bar {
  height: 52px !important;
  min-height: 52px !important;
  padding: 0 18px !important;
  display: flex !important;
  align-items: center !important;
  cursor: pointer !important;
  user-select: none !important;
}

.mobile-chat-apple-sheet .handle-bar-content {
  display: flex !important;
  align-items: center !important;
  justify-content: space-between !important;
  width: 100% !important;
}

.mobile-chat-apple-sheet .preview-left {
  display: flex !important;
  align-items: center !important;
  gap: 12px !important;
  flex: 1 !important;
  min-width: 0 !important;
}

.mobile-chat-apple-sheet .chat-bubble-box {
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  flex-shrink: 0 !important;
}

.mobile-chat-apple-sheet .bubble-icon {
  font-size: 22px !important;
  color: var(--text-primary, #1e293b) !important;
}

.mobile-chat-apple-sheet .preview-text-wrap {
  font-size: 14px !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
  white-space: nowrap !important;
  display: flex !important;
  align-items: center !important;
}

.mobile-chat-apple-sheet .sender-name {
  font-weight: 700 !important;
  color: #2b5797 !important;
}

.mobile-chat-apple-sheet .msg-colon {
  color: #64748b !important;
  margin-right: 4px !important;
}

.mobile-chat-apple-sheet .msg-content {
  color: var(--text-primary, #334155) !important;
  font-weight: 400 !important;
}

.mobile-chat-apple-sheet .placeholder-text {
  color: var(--text-tertiary, #94a3b8) !important;
  font-size: 13px !important;
}

.mobile-chat-apple-sheet .preview-right {
  font-size: 13px !important;
  color: var(--text-secondary, #475569) !important;
  margin-left: 10px !important;
  flex-shrink: 0 !important;
}

/* 抽屉内容区 */
.mobile-chat-apple-sheet .sheet-chat-body {
  flex: 1 !important;
  height: calc(100% - 52px) !important;
  overflow: hidden !important;
  padding-bottom: calc(12px + env(safe-area-inset-bottom)) !important;
}

.mobile-chat-apple-sheet .yuemu-space-right-chat {
  height: 100% !important;
  width: 100% !important;
  border: none !important;
  box-shadow: none !important;
  padding-bottom: 8px !important;
}
/* 经典 12 色系 上下布局 Preset 色盘面板 */
.inline-color-picker {
  margin-top: 16px !important;
  background: var(--card-background, #ffffff) !important;
  border-radius: 16px !important;
  border: 1px solid var(--border-color, rgba(0, 0, 0, 0.08)) !important;
  padding: 16px !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06) !important;
  animation: yuemu-fadeIn 0.3s ease !important;
  width: 100% !important;
  box-sizing: border-box !important;
}

.inline-color-picker .picker-header {
  display: flex !important;
  align-items: center !important;
  justify-content: space-between !important;
  margin-bottom: 14px !important;
}

.inline-color-picker .picker-title {
  font-size: 14px !important;
  font-weight: 700 !important;
  color: var(--text-primary, #1e293b) !important;
  display: flex !important;
  align-items: center !important;
  gap: 6px !important;
}

.inline-color-picker .close-picker {
  background: none !important;
  border: none !important;
  color: var(--text-secondary, #94a3b8) !important;
  cursor: pointer !important;
  font-size: 14px !important;
  padding: 4px !important;
}

.inline-color-picker .picker-body {
  display: flex !important;
  flex-direction: column !important;
  gap: 16px !important;
  width: 100% !important;
  box-sizing: border-box !important;
}

/* 【上层】12 个圆球, 2 行 x 6 个, 左右边缘 100% 精准齐平对齐 */
.preset-swatches-grid {
  display: flex !important;
  flex-direction: column !important;
  gap: 12px !important;
  width: 100% !important;
  box-sizing: border-box !important;
}

.preset-swatches-row {
  display: flex !important;
  align-items: center !important;
  justify-content: space-between !important;
  width: 100% !important;
  box-sizing: border-box !important;
}

.swatch-circle-item {
  width: 32px !important;
  height: 32px !important;
  border-radius: 50% !important;
  cursor: pointer !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  border: 1px solid rgba(0, 0, 0, 0.12) !important;
  transition: transform 0.2s ease, box-shadow 0.2s ease !important;
  flex-shrink: 0 !important;
  box-sizing: border-box !important;
}

.swatch-circle-item:hover {
  transform: scale(1.15) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.18) !important;
}

.swatch-circle-item .check-icon {
  font-size: 12px !important;
}

/* 【下层】自定义 HEX 选色组 (胶囊 + 文本框 + 搜索按钮) */
.custom-color-row {
  display: flex !important;
  align-items: center !important;
  gap: 10px !important;
  width: 100% !important;
  box-sizing: border-box !important;
}

.native-color-picker-wrap {
  width: 36px !important;
  height: 36px !important;
  border-radius: 10px !important;
  position: relative !important;
  overflow: hidden !important;
  border: 1px solid rgba(0, 0, 0, 0.15) !important;
  flex-shrink: 0 !important;
  cursor: pointer !important;
}

.native-color-input {
  position: absolute !important;
  top: -10px !important;
  left: -10px !important;
  width: 60px !important;
  height: 60px !important;
  opacity: 0 !important;
  cursor: pointer !important;
}

.hex-text-input {
  flex: 1 !important;
  min-width: 0 !important;
  height: 36px !important;
  border-radius: 10px !important;
  border: 1px solid var(--border-color, rgba(0, 0, 0, 0.15)) !important;
  padding: 0 12px !important;
  font-size: 14px !important;
  font-family: monospace !important;
  background: var(--card-background, #ffffff) !important;
  color: var(--text-primary, #1e293b) !important;
  outline: none !important;
}

.confirm-color-btn {
  height: 36px !important;
  padding: 0 16px !important;
  border-radius: 10px !important;
  border: none !important;
  background: #2563eb !important;
  color: #ffffff !important;
  font-size: 14px !important;
  font-weight: 600 !important;
  cursor: pointer !important;
  flex-shrink: 0 !important;
  white-space: nowrap !important;
  transition: background 0.2s ease !important;
}

.confirm-color-btn:hover {
  background: #1d4ed8 !important;
}

.confirm-color-btn:hover {
  background: #1d4ed8 !important;
}

.space-list-loading-more {
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  gap: 8px !important;
  padding: 24px 0 40px !important;
  font-size: 14px !important;
  font-weight: 500 !important;
  color: var(--text-secondary, #64748b) !important;
  width: 100% !important;
}

.space-list-loading-more .yuemu-spinner {
  color: #2563eb !important;
}

.space-list-no-more {
  text-align: center !important;
  padding: 24px 0 40px !important;
  font-size: 13px !important;
  color: var(--text-tertiary, #94a3b8) !important;
  font-weight: 500 !important;
  width: 100% !important;
}

/* PC 端分享按钮高晶莹圆形微动效 */
.top-header-right-user {
  display: flex !important;
  align-items: center !important;
  gap: 12px !important;
}

.pc-header-share-btn {
  width: 38px !important;
  height: 38px !important;
  border-radius: 50% !important;
  border: 1px solid var(--border-color, rgba(0, 0, 0, 0.08)) !important;
  background: var(--card-background, #ffffff) !important;
  color: var(--text-primary, #334155) !important;
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  font-size: 15px !important;
  cursor: pointer !important;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1) !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04) !important;
  outline: none !important;
  padding: 0 !important;
  margin: 0 !important;
  flex-shrink: 0 !important;
}

.pc-header-share-btn:hover {
  background: #2563eb !important;
  color: #ffffff !important;
  border-color: #2563eb !important;
  transform: scale(1.08) !important;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.3) !important;
}

.pc-header-share-btn i {
  font-size: 15px !important;
  margin: 0 !important;
  line-height: 1 !important;
  display: inline-block !important;
}

.space-dynamics-panel {
  max-width: 1200px;
  margin: 0 auto;
  padding: 16px 20px 40px;
  box-sizing: border-box;
}

.dynamics-panel-header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: 20px;
}

.space-dynamics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
}

.space-feed-card {
  background: var(--card-background, #ffffff);
  border: 1px solid var(--border-color, #e2e8f0);
  border-radius: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.03);
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.space-feed-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.08);
  border-color: rgba(59, 130, 246, 0.3);
}

.feed-card-media {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 10;
  overflow: hidden;
  background: var(--bg-secondary, #f8fafc);
}

.feed-card-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.35s ease;
}

.space-feed-card:hover .feed-card-img {
  transform: scale(1.05);
}

.feed-card-text-cover {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  box-sizing: border-box;
  color: #ffffff;
  font-weight: 700;
  font-size: 15px;
  text-align: center;
}

.feed-type-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
  backdrop-filter: blur(8px);
  color: #ffffff;
  background: rgba(15, 23, 42, 0.6);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.feed-type-badge.type-picture {
  background: rgba(37, 99, 235, 0.8);
}

.feed-type-badge.type-post {
  background: rgba(124, 58, 237, 0.8);
}

.feed-type-badge.type-activity {
  background: rgba(217, 119, 6, 0.8);
}

.feed-card-body {
  padding: 14px 16px 16px;
  display: flex;
  flex-direction: column;
  flex: 1;
  gap: 8px;
}

.feed-card-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary, #0f172a);
  margin: 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.feed-card-desc {
  font-size: 13px;
  color: var(--text-secondary, #64748b);
  margin: 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.feed-card-footer {
  margin-top: auto;
  padding-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid var(--border-color, #f1f5f9);
}

.feed-author-info {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.feed-author-avatar {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.feed-author-name {
  font-size: 12px;
  color: var(--text-secondary, #64748b);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.feed-meta-stats {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: var(--text-secondary, #94a3b8);
  flex-shrink: 0;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 3px;
}

.activities-empty-state {
  display: flex !important;
  flex-direction: column !important;
  align-items: center !important;
  justify-content: center !important;
  text-align: center !important;
  padding: 48px 24px !important;
  margin: 20px auto !important;
  width: 100% !important;
  box-sizing: border-box !important;
}

.activities-empty-state .empty-icon-circle {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--hover-background, #f1f5f9);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: var(--text-secondary, #94a3b8);
  margin: 0 auto 12px auto;
}

.activities-empty-state .empty-title {
  margin: 0 0 6px 0 !important;
  font-size: 16px !important;
  font-weight: 600 !important;
  color: var(--text-primary, #1e293b) !important;
  text-align: center !important;
}

.activities-empty-state .empty-desc {
  margin: 0 !important;
  font-size: 13px !important;
  color: var(--text-secondary, #64748b) !important;
  text-align: center !important;
}

.space-load-more-btn {
  display: block;
  min-width: 120px;
  margin: 24px auto 0;
  padding: 10px 24px;
  border: 1px solid var(--border-color, #d1d5db);
  border-radius: 99px;
  background: var(--card-background, #ffffff);
  color: var(--text-primary, #111827);
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.space-load-more-btn:hover {
  border-color: #3b82f6;
  color: #2563eb;
  background: #eff6ff;
}

.space-avatar-wrap {
  width: 42px !important;
  height: 42px !important;
  border-radius: 10px !important;
  overflow: hidden !important;
  flex-shrink: 0 !important;
}

.space-card-main {
  flex: 1 !important;
  min-width: 0 !important;
  display: flex !important;
  flex-direction: column !important;
  justify-content: center !important;
  gap: 4px !important;
}

.space-title-row {
  display: flex !important;
  align-items: center !important;
  width: 100% !important;
}

.space-name {
  font-size: 14px !important;
  font-weight: 600 !important;
  color: var(--text-primary) !important;
  white-space: nowrap !important;
  overflow: hidden !important;
  text-overflow: ellipsis !important;
  width: 100% !important;
  text-align: left !important;
}

.space-sub-row {
  display: flex !important;
  align-items: center !important;
  justify-content: space-between !important;
  width: 100% !important;
  gap: 8px !important;
}

.space-tags-row {
  display: flex !important;
  flex-direction: row !important;
  align-items: center !important;
  gap: 4px !important;
  margin-left: auto !important;
  flex-shrink: 0 !important;
}

.meta-text {
  font-size: 12px !important;
  color: var(--text-secondary) !important;
  display: block !important;
  text-align: left !important;
  line-height: 1.2 !important;
}

.space-dynamics-panel {
  width: min(760px, 100%);
  margin: 0 auto;
  padding: 20px 16px 40px;
  box-sizing: border-box;
}

.activities-empty-state {
  display: flex !important;
  flex-direction: column !important;
  align-items: center !important;
  justify-content: center !important;
  text-align: center !important;
  padding: 48px 24px !important;
  margin: 20px auto !important;
  width: 100% !important;
  box-sizing: border-box !important;
}

.activities-empty-state .empty-icon-circle {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--hover-background, #f1f5f9);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: var(--text-secondary, #94a3b8);
  margin: 0 auto 12px auto;
}

.activities-empty-state .empty-title {
  margin: 0 0 6px 0 !important;
  font-size: 16px !important;
  font-weight: 600 !important;
  color: var(--text-primary, #1e293b) !important;
  text-align: center !important;
}

.activities-empty-state .empty-desc {
  margin: 0 !important;
  font-size: 13px !important;
  color: var(--text-secondary, #64748b) !important;
  text-align: center !important;
}

/* ─────────────────────────────────────────────────────────────
   右下角方向盘 ⭕️ 快捷胶囊创作菜单 (FAB Radial Pill Menu)
   ───────────────────────────────────────────────────────────── */
.fab-radial-container {
  position: fixed;
  right: 28px;
  bottom: 80px;
  z-index: 1000;
}

.fab-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.35);
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px);
  z-index: 999;
}

.fab-main-btn {
  position: relative;
  z-index: 1001;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #1e293b;
  color: #ffffff;
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.35);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  transition: all 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.fab-main-btn:hover {
  background: #334155;
  transform: scale(1.08);
}

.fab-main-btn.is-open {
  background: #0f172a;
  transform: rotate(135deg);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.25);
}

/* ─────────────────────────────────────────────────────────────
   右下角高奢纵向阶梯胶囊创作菜单 (FAB Floating Pill Stack)
   ───────────────────────────────────────────────────────────── */
.fab-radial-container {
  position: fixed;
  right: 28px;
  bottom: 80px;
  width: 56px;
  height: 56px;
  z-index: 1000;
}

.fab-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.38);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  z-index: 999;
}

.fab-main-btn {
  position: relative;
  z-index: 1001;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #1e293b;
  color: #ffffff;
  border: 1px solid rgba(255, 255, 255, 0.15);
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.38);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  transition: all 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.fab-main-btn:hover {
  background: #334155;
  transform: scale(1.08);
}

.fab-main-btn.is-open {
  background: #0f172a;
  transform: rotate(135deg);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.25);
}

.fab-pill-stack {
  position: absolute;
  bottom: 0;
  right: 0;
  z-index: 1000;
  pointer-events: none;
}

.fab-radial-container.is-active .fab-pill-stack {
  pointer-events: auto;
}

.fab-pill-item {
  position: absolute;
  bottom: 6px;
  right: 0;
  opacity: 0;
  transform: translateY(20px) scale(0.6);
  transition: all 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
  white-space: nowrap;
  cursor: pointer;
}

/* 按钮 1：上传图片 (第一层 向上 -66px) */
.fab-pill-item.item-picture.is-visible {
  opacity: 1;
  transform: translateY(-66px) scale(1);
  transition-delay: 0.04s;
}

/* 按钮 2：发布动态 (第二层 向上 -122px) */
.fab-pill-item.item-post.is-visible {
  opacity: 1;
  transform: translateY(-122px) scale(1);
  transition-delay: 0.08s;
}

/* 按钮 3：创建活动 (第三层 向上 -178px) */
.fab-pill-item.item-activity.is-visible {
  opacity: 1;
  transform: translateY(-178px) scale(1);
  transition-delay: 0.12s;
}

/* 高奢晶莹胶囊卡片 */
.fab-pill-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 20px;
  border-radius: 30px;
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.btn-picture {
  background: linear-gradient(135deg, #334155 0%, #1e293b 100%);
}

.btn-post {
  background: linear-gradient(135deg, #475569 0%, #334155 100%);
}

.btn-activity {
  background: linear-gradient(135deg, #64748b 0%, #475569 100%);
}

.fab-pill-btn:hover {
  transform: scale(1.06) translateX(-4px);
  filter: brightness(1.18);
  box-shadow: 0 14px 32px rgba(0, 0, 0, 0.4);
}

.fab-pill-btn i {
  font-size: 16px;
}

/* 成员页 Header 操作组与排行榜按钮 */
.header-right-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-rank-btn {
  background: rgba(245, 158, 11, 0.08);
  border: 1px solid rgba(245, 158, 11, 0.2);
  color: #d97706;
  font-size: 13px;
  font-weight: 600;
  padding: 6px 14px;
  border-radius: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s ease;
}

.header-rank-btn:hover {
  background: rgba(245, 158, 11, 0.16);
  border-color: rgba(245, 158, 11, 0.4);
  transform: translateY(-1px);
}

.rank-star-icon {
  color: #f59e0b;
}

/* 活跃成员排行榜 Modal 弹窗 */
.yuemu-rank-modal .ant-modal-content {
  border-radius: 24px;
  padding: 24px;
  overflow: hidden;
  background: var(--card-background, #ffffff);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.12);
}

.rank-modal-container {
  display: flex;
  flex-direction: column;
}

.rank-modal-header {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-color, #e2e8f0);
}

.rank-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rank-trophy-icon {
  color: #f59e0b;
  font-size: 20px;
}

.rank-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary, #0f172a);
}

.rank-sub {
  font-size: 12px;
  color: var(--text-secondary, #64748b);
  margin-top: 4px;
  display: block;
}

.rank-modal-scroll-area {
  max-height: 420px;
  overflow-y: auto;
  padding-right: 4px;
}

.rank-members-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rank-member-row {
  display: flex;
  align-items: center;
  padding: 10px 14px;
  border-radius: 14px;
  background: var(--hover-background, #f8fafc);
  border: 1px solid var(--border-color, #f1f5f9);
  cursor: pointer;
  transition: all 0.2s ease;
}

.rank-member-row:hover {
  background: var(--card-background, #ffffff);
  border-color: #cbd5e1;
  transform: translateX(4px);
}

.rank-num-badge {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  margin-right: 12px;
  flex-shrink: 0;
  background: var(--border-color, #e2e8f0);
  color: var(--text-secondary, #64748b);
}

.rank-num-badge.rank-1 {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  color: #ffffff;
}

.rank-num-badge.rank-2 {
  background: linear-gradient(135deg, #94a3b8, #64748b);
  color: #ffffff;
}

.rank-num-badge.rank-3 {
  background: linear-gradient(135deg, #b45309, #78350f);
  color: #ffffff;
}

.rank-crown-gold { color: #fff; font-size: 13px; }
.rank-crown-silver { color: #fff; font-size: 13px; }
.rank-crown-bronze { color: #fff; font-size: 13px; }

.rank-member-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  object-fit: cover;
  margin-right: 12px;
  flex-shrink: 0;
}

.rank-member-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.rank-member-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary, #0f172a);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rank-member-role {
  font-size: 11px;
  color: var(--text-secondary, #64748b);
}

.rank-member-score {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 700;
  color: #d97706;
  flex-shrink: 0;
}

.rank-footer-bar {
  text-align: center;
  padding: 12px 0 4px;
  font-size: 12px;
  color: var(--text-secondary, #64748b);
}

.rank-loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.rank-modal-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 42px 24px;
  text-align: center;
}

/* 搜索弹窗高级未搜索探索引导 / 空状态 (居中高奢排版) */
.search-initial-hint,
.space-search-empty {
  display: flex !important;
  flex-direction: column !important;
  align-items: center !important;
  justify-content: center !important;
  padding: 50px 24px !important;
  text-align: center !important;
  margin: 30px auto !important;
  width: 100% !important;
  box-sizing: border-box !important;
}

.search-initial-hint .empty-icon-circle,
.space-search-empty .empty-icon-circle {
  width: 64px !important;
  height: 64px !important;
  border-radius: 50% !important;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.1) 0%, rgba(59, 130, 246, 0.2) 100%) !important;
  color: var(--link-color, #2563eb) !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  font-size: 26px !important;
  margin-bottom: 16px !important;
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.12) !important;
}

.search-initial-hint .empty-title,
.space-search-empty .empty-title {
  margin: 0 0 8px 0 !important;
  font-size: 17px !important;
  font-weight: 700 !important;
  color: var(--text-primary, #0f172a) !important;
}

.search-initial-hint .empty-desc,
.space-search-empty .empty-desc {
  margin: 0 !important;
  font-size: 13px !important;
  color: var(--text-secondary, #64748b) !important;
  max-width: 320px !important;
  line-height: 1.6 !important;
}

.space-search-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  overflow-x: auto;
}

.space-search-tab {
  min-width: 64px;
  height: 34px;
  padding: 0 12px;
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 6px;
  background: transparent;
  color: var(--text-secondary, #64748b);
  cursor: pointer;
}

.space-search-tab.active {
  border-color: #3b82f6;
  background: #eff6ff;
  color: #2563eb;
  font-weight: 600;
}

.space-search-loading,
.space-search-empty {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 120px;
  color: var(--text-secondary, #64748b);
}

.space-search-results {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: 10px;
  margin-top: 18px;
}

.space-search-result {
  display: flex;
  min-width: 0;
  padding: 8px;
  gap: 10px;
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 8px;
  background: var(--card-background, #fff);
  text-align: left;
  cursor: pointer;
}

.space-search-result:hover { border-color: #93c5fd; }
.space-search-result img,
.space-search-result-placeholder {
  width: 64px;
  height: 64px;
  border-radius: 6px;
  object-fit: cover;
  flex: 0 0 64px;
}
.space-search-result-placeholder {
  display: grid;
  place-items: center;
  background: #f1f5f9;
  color: #64748b;
}
.space-search-result-main { min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.space-search-result-main strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--text-primary, #1f2937); }
.space-search-result-main p { margin: 0; color: var(--text-secondary, #64748b); font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.space-search-result-type { color: #2563eb; font-size: 12px; }
.space-search-more { display: block; margin: 16px auto 0; border: 0; background: transparent; color: #2563eb; cursor: pointer; }

@media (max-width: 768px) {
  .space-search-results { grid-template-columns: 1fr; }
}
</style>
