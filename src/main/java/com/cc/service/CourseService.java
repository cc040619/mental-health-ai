package com.cc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.request.ChapterSaveDTO;
import com.cc.dto.request.ChapterSortDTO;
import com.cc.dto.request.CourseCommentCreateDTO;
import com.cc.dto.request.CourseProgressUpdateDTO;
import com.cc.dto.request.CourseSaveDTO;
import com.cc.dto.request.PracticeCheckinDTO;
import com.cc.dto.response.AdminChapterResponseDTO;
import com.cc.dto.response.AdminCourseCommentResponseDTO;
import com.cc.dto.response.AdminCourseDetailResponseDTO;
import com.cc.dto.response.AdminCourseListResponseDTO;
import com.cc.dto.response.CheckinCalendarResponseDTO;
import com.cc.dto.response.CourseCommentResponseDTO;
import com.cc.dto.response.CourseDetailResponseDTO;
import com.cc.dto.response.CourseListResponseDTO;
import com.cc.dto.response.CourseMineResponseDTO;
import com.cc.dto.response.CourseStatsResponseDTO;
import com.cc.dto.response.DailyPracticeResponseDTO;
import com.cc.entity.Course;
import com.cc.entity.CourseChapter;
import com.cc.entity.CourseComment;
import com.cc.entity.CourseFavorite;
import com.cc.entity.CourseProgress;
import com.cc.entity.PracticeCheckin;
import com.cc.entity.User;
import com.cc.enums.CourseCategory;
import com.cc.enums.CourseStatus;
import com.cc.exception.BusinessException;
import com.cc.mapper.CourseChapterMapper;
import com.cc.mapper.CourseCommentMapper;
import com.cc.mapper.CourseFavoriteMapper;
import com.cc.mapper.CourseMapper;
import com.cc.mapper.CourseProgressMapper;
import com.cc.mapper.PracticeCheckinMapper;
import com.cc.mapper.UserMapper;
import com.cc.service.convert.CourseConvert;
import com.cc.util.CurrentUserUtils;
import com.cc.util.SensitiveWordFilter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 心理课程业务层：广场/详情/收藏/评论/我的课程/每日推荐/打卡/进度
 *
 * <p>安全合规：进度/打卡只操作当前用户数据；评论删除校验本人或管理员；
 * 日志禁止打印评论内容与打卡数据，只记 userId 与操作类型</p>
 *
 * @author cc
 * @date 2026-09-09
 */
@Slf4j
@Service
public class CourseService {

    @Resource
    private CourseMapper courseMapper;
    @Resource
    private CourseChapterMapper chapterMapper;
    @Resource
    private CourseFavoriteMapper favoriteMapper;
    @Resource
    private CourseProgressMapper progressMapper;
    @Resource
    private CourseCommentMapper commentMapper;
    @Resource
    private PracticeCheckinMapper checkinMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private CourseConvert convert;
    @Resource
    private SensitiveWordFilter sensitiveWordFilter;

    // ==================== 课程广场 ====================

    /**
     * 课程广场列表（类别筛选 + 分页）
     * <p>isLearning/isFavorite 用 IN 批量查询后内存组装，禁止 N+1
     */
    public Map<String, Object> listCourses(String category, Long current, Long size) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        long cur = (current != null && current > 0) ? current : 1L;
        long sz = (size != null && size > 0) ? Math.min(size, 50L) : 12L;

        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        // ponytail: 状态迁移后用户端只展示 PUBLISHED(2)，原 status=1 升级为 2
        wrapper.eq(Course::getStatus, CourseStatus.PUBLISHED.getCode());
        if (category != null && !category.isBlank()) {
            if (CourseCategory.fromCode(category) == null) {
                throw new BusinessException("课程类别不存在");
            }
            wrapper.eq(Course::getCategory, category);
        }
        wrapper.orderByDesc(Course::getCreatedAt);

        Page<Course> page = new Page<>(cur, sz);
        Page<Course> result = courseMapper.selectPage(page, wrapper);
        List<Course> courses = result.getRecords();
        List<Long> courseIds = courses.stream().map(Course::getId).toList();

        // 批量查 isLearning / isFavorite
        Set<Long> learnedIds = courseIds.isEmpty()
                ? Set.of() : new HashSet<>(progressMapper.selectLearnedCourseIds(userId, courseIds));
        Set<Long> favoriteIds = courseIds.isEmpty()
                ? Set.of() : new HashSet<>(favoriteMapper.selectFavoriteCourseIds(userId, courseIds));

        List<CourseListResponseDTO> list = courses.stream()
                .map(c -> convert.toListDTO(c, favoriteIds.contains(c.getId()), learnedIds.contains(c.getId()), null))
                .toList();
        return Map.of("records", list, "total", result.getTotal());
    }

    /**
     * 课程详情：介绍 + 章节列表（含进度）+ 完成度 + 收藏 + 评论数
     * <p>章节进度一次查出（按 user+course 批量），禁止逐章查询
     */
    public CourseDetailResponseDTO getCourseDetail(Long courseId) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        Course course = getOnlineCourse(courseId);

        List<CourseChapter> chapters = chapterMapper.selectByCourseId(courseId);
        List<Long> chapterIds = chapters.stream().map(CourseChapter::getId).toList();

        List<CourseProgress> userProgress = chapterIds.isEmpty()
                ? List.of() : progressMapper.selectList(new LambdaQueryWrapper<CourseProgress>()
                        .eq(CourseProgress::getUserId, userId)
                        .in(CourseProgress::getChapterId, chapterIds));

        boolean isFavorite = favoriteMapper.selectCount(new LambdaQueryWrapper<CourseFavorite>()
                .eq(CourseFavorite::getUserId, userId)
                .eq(CourseFavorite::getCourseId, courseId)) > 0;

        long commentCount = commentMapper.selectCount(new LambdaQueryWrapper<CourseComment>()
                .eq(CourseComment::getCourseId, courseId));

        return convert.toDetailDTO(course, chapters, userProgress, isFavorite, commentCount);
    }

    // ==================== 收藏 ====================

    /**
     * 收藏 toggle，返回最新状态
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean toggleFavorite(Long courseId) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        getOnlineCourse(courseId);

        LambdaQueryWrapper<CourseFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseFavorite::getUserId, userId).eq(CourseFavorite::getCourseId, courseId);
        CourseFavorite existing = favoriteMapper.selectOne(wrapper);

        if (existing != null) {
            favoriteMapper.deleteById(existing.getId());
            log.info("用户取消收藏课程: userId={}, courseId={}", userId, courseId);
            return false;
        }
        CourseFavorite fav = CourseFavorite.builder()
                .userId(userId).courseId(courseId).createdAt(LocalDateTime.now()).build();
        favoriteMapper.insert(fav);
        log.info("用户收藏课程: userId={}, courseId={}", userId, courseId);
        return true;
    }

    /**
     * 我的收藏课程列表
     */
    public Map<String, Object> listFavorites(Long current, Long size) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        long cur = (current != null && current > 0) ? current : 1L;
        long sz = (size != null && size > 0) ? Math.min(size, 50L) : 12L;

        Page<CourseListResponseDTO> page = new Page<>(cur, sz);
        IPage<CourseListResponseDTO> result = favoriteMapper.selectFavoritesPage(page, userId);
        List<CourseListResponseDTO> records = result.getRecords();

        // 补充 categoryLabel / totalDurationText / isLearning
        // selectFavoritesPage 仅 select 了基础列（total_duration 已映射到 totalDuration）
        List<Long> courseIds = records.stream().map(CourseListResponseDTO::getId).toList();
        Set<Long> learnedIds = courseIds.isEmpty()
                ? Set.of() : new HashSet<>(progressMapper.selectLearnedCourseIds(userId, courseIds));

        records.forEach(dto -> {
            CourseCategory cat = CourseCategory.fromCode(dto.getCategory());
            dto.setCategoryLabel(cat != null ? cat.getLabel() : dto.getCategory());
            dto.setTotalDurationText(convert.formatDuration(dto.getTotalDuration()));
            dto.setIsFavorite(true);
            dto.setIsLearning(learnedIds.contains(dto.getId()));
        });
        return Map.of("records", records, "total", result.getTotal());
    }

    // ==================== 评论 ====================

    /**
     * 评论分页（含昵称/头像/是否本人）
     */
    public Map<String, Object> listComments(Long courseId, Long current, Long size) {
        getOnlineCourse(courseId);
        Long userId = CurrentUserUtils.getCurrentUserId();
        long cur = (current != null && current > 0) ? current : 1L;
        long sz = (size != null && size > 0) ? Math.min(size, 50L) : 10L;

        Page<CourseCommentResponseDTO> page = new Page<>(cur, sz);
        IPage<CourseCommentResponseDTO> result = commentMapper.selectCommentPage(page, courseId, userId);
        return Map.of("records", result.getRecords(), "total", result.getTotal());
    }

    /**
     * 发表评论
     */
    @Transactional(rollbackFor = Exception.class)
    public CourseCommentResponseDTO createComment(Long courseId, CourseCommentCreateDTO dto) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        getOnlineCourse(courseId);

        CourseComment comment = CourseComment.builder()
                .courseId(courseId)
                .userId(userId)
                .content(dto.getContent())
                .createdAt(LocalDateTime.now())
                .build();
        commentMapper.insert(comment);
        log.info("用户发表课程评论: userId={}, courseId={}, commentId={}", userId, courseId, comment.getId());

        User user = userMapper.selectById(userId);
        CourseCommentResponseDTO resp = new CourseCommentResponseDTO();
        resp.setId(comment.getId());
        resp.setCourseId(courseId);
        resp.setUserId(userId);
        resp.setContent(comment.getContent());
        resp.setCreatedAt(comment.getCreatedAt());
        resp.setNickname(user != null ? user.getNickname() : null);
        resp.setAvatar(user != null ? user.getAvatar() : null);
        resp.setIsMine(true);
        return resp;
    }

    /**
     * 删除评论（仅本人或管理员）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        CourseComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        boolean isOwner = comment.getUserId().equals(userId);
        boolean isAdmin = CurrentUserUtils.isAdmin();
        if (!isOwner && !isAdmin) {
            throw new BusinessException("只能删除自己的评论");
        }
        commentMapper.deleteById(commentId);
        log.info("删除课程评论: userId={}, commentId={}, asAdmin={}", userId, commentId, isAdmin);
    }

    // ==================== 进度上报 ====================

    /**
     * 上报章节学习进度：upsert（唯一 user+chapter），首次学习 course.learn_count+1，达标置 is_finished
     * <p>完成判定：progressSeconds >= duration - 10 或 >= duration * 0.9
     *
     * @return Map {isFinished, completion} 便于前端刷新 UI
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateProgress(CourseProgressUpdateDTO dto) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        CourseChapter chapter = chapterMapper.selectById(dto.getChapterId());
        if (chapter == null) {
            throw new BusinessException("章节不存在");
        }
        int duration = chapter.getDuration() == null ? 0 : chapter.getDuration();
        int progress = dto.getProgressSeconds() == null ? 0 : dto.getProgressSeconds();
        if (progress < 0 || (duration > 0 && progress > duration)) {
            throw new BusinessException("播放进度超出章节时长范围");
        }

        // 完成判定
        boolean finished = duration > 0
                && (progress >= duration - 10 || progress >= duration * 0.9);

        LambdaQueryWrapper<CourseProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseProgress::getUserId, userId).eq(CourseProgress::getChapterId, dto.getChapterId());
        CourseProgress existing = progressMapper.selectOne(wrapper);

        boolean isFirstLearnForCourse = existing == null
                && !hasAnyProgressForCourse(userId, chapter.getCourseId());

        if (existing != null) {
            existing.setProgressSeconds(progress);
            existing.setDuration(duration);
            // 已完成保持 1，否则按本次判定（不回退已完成状态）
            int priorFinished = existing.getIsFinished() == null ? 0 : existing.getIsFinished();
            existing.setIsFinished(finished || priorFinished == 1 ? 1 : 0);
            existing.setLastPlayedAt(LocalDateTime.now());
            progressMapper.updateById(existing);
        } else {
            CourseProgress cp = CourseProgress.builder()
                    .userId(userId)
                    .chapterId(dto.getChapterId())
                    .progressSeconds(progress)
                    .duration(duration)
                    .isFinished(finished ? 1 : 0)
                    .lastPlayedAt(LocalDateTime.now())
                    .build();
            progressMapper.insert(cp);
        }

        // 首次学习该课程 → learn_count + 1（幂等：基于上面是否已存在进度判断）
        if (isFirstLearnForCourse) {
            Course course = courseMapper.selectById(chapter.getCourseId());
            if (course != null) {
                int newCount = (course.getLearnCount() == null ? 0 : course.getLearnCount()) + 1;
                course.setLearnCount(newCount);
                courseMapper.updateById(course);
            }
        }

        // 返回当前课程完成度
        int completion = computeCourseCompletion(userId, chapter.getCourseId());
        log.info("上报学习进度: userId={}, chapterId={}, progress={}s, finished={}, completion={}%",
                userId, dto.getChapterId(), progress, finished, completion);
        return Map.of("isFinished", finished, "completion", completion);
    }

    // ==================== 每日一练 / 打卡 ====================

    /**
     * 今日推荐：从 MEDITATION 类别课程章节中按日期轮询取一个章节
     */
    public DailyPracticeResponseDTO getDailyPractice() {
        Long userId = CurrentUserUtils.getCurrentUserId();

        // 取所有正念冥想类上架课程的章节（按课程id + sortOrder 排序后扁平化）
        LambdaQueryWrapper<Course> courseWrapper = new LambdaQueryWrapper<>();
        courseWrapper.eq(Course::getStatus, CourseStatus.PUBLISHED.getCode()).eq(Course::getCategory, CourseCategory.MEDITATION.getCode())
                .orderByAsc(Course::getId);
        List<Course> meditationCourses = courseMapper.selectList(courseWrapper);
        if (meditationCourses.isEmpty()) {
            throw new BusinessException("暂无可推荐的冥想练习");
        }

        List<Long> courseIds = meditationCourses.stream().map(Course::getId).toList();
        LambdaQueryWrapper<CourseChapter> chWrapper = new LambdaQueryWrapper<>();
        chWrapper.in(CourseChapter::getCourseId, courseIds)
                .orderByAsc(CourseChapter::getCourseId).orderByAsc(CourseChapter::getSortOrder);
        List<CourseChapter> chapters = chapterMapper.selectList(chWrapper);
        if (chapters.isEmpty()) {
            throw new BusinessException("暂无可推荐的冥想练习");
        }

        // 按日期轮询取章节
        long epochDay = LocalDate.now().toEpochDay();
        int index = (int) (Math.floorMod(epochDay, chapters.size()));
        CourseChapter picked = chapters.get(index);
        Course course = meditationCourses.stream()
                .filter(c -> c.getId().equals(picked.getCourseId())).findFirst()
                .orElse(meditationCourses.get(0));

        boolean checkedInToday = checkinMapper.selectCount(new LambdaQueryWrapper<PracticeCheckin>()
                .eq(PracticeCheckin::getUserId, userId)
                .eq(PracticeCheckin::getCheckinDate, LocalDate.now())) > 0;

        DailyPracticeResponseDTO dto = new DailyPracticeResponseDTO();
        dto.setCourseId(course.getId());
        dto.setCourseTitle(course.getTitle());
        dto.setCourseCover(course.getCoverUrl());
        dto.setCategory(course.getCategory());
        CourseCategory cat = CourseCategory.fromCode(course.getCategory());
        dto.setCategoryLabel(cat != null ? cat.getLabel() : course.getCategory());
        dto.setChapterId(picked.getId());
        dto.setChapterTitle(picked.getTitle());
        dto.setAudioUrl(picked.getAudioUrl());
        dto.setDuration(picked.getDuration());
        dto.setDurationText(convert.formatDuration(picked.getDuration()));
        dto.setIntro(picked.getIntro());
        dto.setHasCheckedInToday(checkedInToday);
        return dto;
    }

    /**
     * 每日打卡：服务端校验当日该章节进度已播 ≥60 秒
     */
    @Transactional(rollbackFor = Exception.class)
    public void checkin(PracticeCheckinDTO dto) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        LocalDate today = LocalDate.now();

        // 唯一 user+date，重复打卡拒绝
        boolean already = checkinMapper.selectCount(new LambdaQueryWrapper<PracticeCheckin>()
                .eq(PracticeCheckin::getUserId, userId)
                .eq(PracticeCheckin::getCheckinDate, today)) > 0;
        if (already) {
            throw new BusinessException("今日已打卡，明天再来吧");
        }

        // 校验章节存在
        CourseChapter chapter = chapterMapper.selectById(dto.getChapterId());
        if (chapter == null) {
            throw new BusinessException("章节不存在");
        }

        // 防刷：当日该章节进度已播 ≥60 秒
        CourseProgress progress = progressMapper.selectOne(new LambdaQueryWrapper<CourseProgress>()
                .eq(CourseProgress::getUserId, userId)
                .eq(CourseProgress::getChapterId, dto.getChapterId()));
        boolean playedEnough = progress != null
                && progress.getProgressSeconds() != null
                && progress.getProgressSeconds() >= 60
                && progress.getLastPlayedAt() != null
                && progress.getLastPlayedAt().toLocalDate().equals(today);
        if (!playedEnough) {
            throw new BusinessException("请先完成今日练习（播放满 60 秒）再打卡");
        }

        PracticeCheckin checkin = PracticeCheckin.builder()
                .userId(userId)
                .checkinDate(today)
                .courseId(dto.getCourseId())
                .chapterId(dto.getChapterId())
                .duration(dto.getDuration() == null ? 0 : dto.getDuration())
                .createdAt(LocalDateTime.now())
                .build();
        checkinMapper.insert(checkin);
        log.info("用户每日打卡: userId={}, courseId={}", userId, dto.getCourseId());
    }

    /**
     * 当月打卡日历数据 + 当前连续打卡天数
     */
    public Map<String, Object> getCheckinCalendar(String month) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        YearMonth ym;
        try {
            ym = (month == null || month.isBlank()) ? YearMonth.now() : YearMonth.parse(month);
        } catch (Exception e) {
            throw new BusinessException("月份格式应为 YYYY-MM");
        }
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        List<CheckinCalendarResponseDTO> records = checkinMapper.selectMonthCheckins(userId, start, end);

        // streak：从今天往前连续打卡天数（今天未打卡则 streak=0）
        List<PracticeCheckin> allCheckins = checkinMapper.selectList(new LambdaQueryWrapper<PracticeCheckin>()
                .eq(PracticeCheckin::getUserId, userId)
                .orderByDesc(PracticeCheckin::getCheckinDate));
        Set<LocalDate> dates = allCheckins.stream()
                .map(PracticeCheckin::getCheckinDate)
                .collect(Collectors.toSet());
        int streak = 0;
        LocalDate day = LocalDate.now();
        while (dates.contains(day)) {
            streak++;
            day = day.minusDays(1);
        }
        return Map.of("records", records, "streak", streak);
    }

    // ==================== 我的课程 ====================

    /**
     * 我的课程：已学习(completion=100) / 学习中(0<completion<100) / 已收藏
     */
    public CourseMineResponseDTO listMine() {
        Long userId = CurrentUserUtils.getCurrentUserId();

        // 1. 用户有进度的所有章节
        List<CourseProgress> myProgress = progressMapper.selectList(
                new LambdaQueryWrapper<CourseProgress>().eq(CourseProgress::getUserId, userId));
        Set<Long> touchedChapterIds = myProgress.stream().map(CourseProgress::getChapterId).collect(Collectors.toSet());

        // 2. 收藏的课程
        List<CourseFavorite> favorites = favoriteMapper.selectList(
                new LambdaQueryWrapper<CourseFavorite>().eq(CourseFavorite::getUserId, userId));
        Set<Long> favoriteCourseIds = favorites.stream().map(CourseFavorite::getCourseId).collect(Collectors.toSet());

        // 3. 涉及到的课程ID（有进度的章节所属课程 + 收藏课程）
        Set<Long> courseIds = new HashSet<>(favoriteCourseIds);
        if (!touchedChapterIds.isEmpty()) {
            List<CourseChapter> touchedChapters = chapterMapper.selectBatchIds(touchedChapterIds);
            courseIds.addAll(touchedChapters.stream().map(CourseChapter::getCourseId).collect(Collectors.toSet()));
        }
        if (courseIds.isEmpty()) {
            CourseMineResponseDTO empty = new CourseMineResponseDTO();
            empty.setLearned(List.of());
            empty.setLearning(List.of());
            empty.setFavorites(List.of());
            return empty;
        }

        // 4. 一次性加载课程 + 全部章节
        List<Course> courses = courseMapper.selectBatchIds(courseIds);
        List<Long> courseIdList = courses.stream().map(Course::getId).toList();
        List<CourseChapter> allChapters = chapterMapper.selectList(
                new LambdaQueryWrapper<CourseChapter>().in(CourseChapter::getCourseId, courseIdList));
        Map<Long, List<CourseChapter>> chaptersByCourse = allChapters.stream()
                .collect(Collectors.groupingBy(CourseChapter::getCourseId));

        // 5. 按 chapter 分组进度，计算每门课完成度
        Map<Long, CourseProgress> progressByChapter = myProgress.stream()
                .collect(Collectors.toMap(CourseProgress::getChapterId, p -> p, (a, b) -> a));

        List<CourseListResponseDTO> learned = new ArrayList<>();
        List<CourseListResponseDTO> learning = new ArrayList<>();
        List<CourseListResponseDTO> favList = new ArrayList<>();

        for (Course course : courses) {
            List<CourseChapter> chs = chaptersByCourse.getOrDefault(course.getId(), List.of());
            int completion = convert.calcCompletion(chs, progressByChapter);
            boolean isFav = favoriteCourseIds.contains(course.getId());
            CourseListResponseDTO dto = convert.toListDTO(course, isFav, true, completion);

            if (completion >= 100) {
                learned.add(dto);
            } else if (completion > 0) {
                learning.add(dto);
            }
            if (isFav) {
                favList.add(dto);
            }
        }

        CourseMineResponseDTO result = new CourseMineResponseDTO();
        result.setLearned(learned);
        result.setLearning(learning);
        result.setFavorites(favList);
        return result;
    }

    // ==================== 工具方法 ====================

    /** 获取已发布课程，不存在/未发布抛业务异常 */
    private Course getOnlineCourse(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null || course.getStatus() == null
                || !course.getStatus().equals(CourseStatus.PUBLISHED.getCode())) {
            throw new BusinessException("课程不存在或已下架");
        }
        return course;
    }

    /** 判断用户是否对该课程已有任何进度记录 */
    private boolean hasAnyProgressForCourse(Long userId, Long courseId) {
        List<CourseChapter> chapters = chapterMapper.selectByCourseId(courseId);
        if (chapters.isEmpty()) {
            return false;
        }
        List<Long> chapterIds = chapters.stream().map(CourseChapter::getId).toList();
        long cnt = progressMapper.selectCount(new LambdaQueryWrapper<CourseProgress>()
                .eq(CourseProgress::getUserId, userId)
                .in(CourseProgress::getChapterId, chapterIds));
        return cnt > 0;
    }

    /** 计算当前用户对该课程的完成度百分比 */
    private int computeCourseCompletion(Long userId, Long courseId) {
        List<CourseChapter> chapters = chapterMapper.selectByCourseId(courseId);
        if (chapters.isEmpty()) {
            return 0;
        }
        List<Long> chapterIds = chapters.stream().map(CourseChapter::getId).toList();
        List<CourseProgress> progress = progressMapper.selectList(new LambdaQueryWrapper<CourseProgress>()
                .eq(CourseProgress::getUserId, userId)
                .in(CourseProgress::getChapterId, chapterIds));
        Map<Long, CourseProgress> progressMap = progress.stream()
                .collect(Collectors.toMap(CourseProgress::getChapterId, p -> p, (a, b) -> a));
        return convert.calcCompletion(chapters, progressMap);
    }

    // ponytail: 我的课程一次性加载所有相关课程+章节+进度（4 查询），
    // 数据规模上来时可改为按完成度分页查询，当前最短可用路径。

    // ==================== 管理端：课程列表 / 详情 / 创建 / 编辑 / 审核 ====================

    /**
     * 管理端课程列表：聚合 章节数/总时长 一次 SQL 完成，禁止 N+1
     * <p>size ≤ 20，命中 idx_status_created 索引</p>
     */
    public Map<String, Object> adminPage(String keyword, String category, Integer status,
                                         long page, long size) {
        long cur = Math.max(page, 1L);
        long sz = Math.min(Math.max(size, 1L), 20L);

        // 状态入参校验
        if (status != null && CourseStatus.fromCode(status) == null) {
            throw new BusinessException("课程状态不合法");
        }
        // 类别入参校验
        if (StringUtils.hasText(category) && CourseCategory.fromCode(category) == null) {
            throw new BusinessException("课程类别不存在");
        }

        Page<AdminCourseListResponseDTO> pageParam = new Page<>(cur, sz);
        IPage<AdminCourseListResponseDTO> result = courseMapper.selectAdminPage(
                pageParam, trimKw(keyword), StringUtils.hasText(category) ? category : null, status);
        List<AdminCourseListResponseDTO> records = result.getRecords().stream()
                .map(convert::toAdminListDTO)
                .toList();
        return Map.of("records", records, "total", result.getTotal());
    }

    /**
     * 管理端各状态课程计数（前端 Tab 数字）
     */
    public Map<Integer, Long> adminStatusCount() {
        Map<Integer, Long> counts = new HashMap<>();
        // ponytail: 五态初始化为 0，避免前端 Tab 显示空白
        for (CourseStatus s : CourseStatus.values()) {
            counts.put(s.getCode(), 0L);
        }
        for (Map<String, Object> row : courseMapper.selectStatusCount()) {
            counts.put(((Number) row.get("status")).intValue(),
                    ((Number) row.get("cnt")).longValue());
        }
        return counts;
    }

    /**
     * 管理端课程详情：基本信息 + 章节列表（按 sortOrder 升序）
     */
    public AdminCourseDetailResponseDTO adminDetail(Long courseId) {
        Course course = mustGetCourse(courseId);
        List<CourseChapter> chapters = chapterMapper.selectByCourseId(courseId);
        return convert.toAdminDetailDTO(course, chapters);
    }

    /**
     * 管理端创建课程：默认 status=DRAFT(0)，totalDuration=0（章节增删后重算）
     */
    @Transactional(rollbackFor = Exception.class)
    public Long adminSave(CourseSaveDTO dto) {
        validateSaveDTO(dto);
        Course course = Course.builder()
                .title(dto.getTitle().trim())
                .category(dto.getCategory())
                .description(StringUtils.hasText(dto.getDescription()) ? dto.getDescription().trim() : null)
                .coverUrl(dto.getCoverUrl())
                .lecturer(StringUtils.hasText(dto.getLecturer()) ? dto.getLecturer().trim() : null)
                .targetAudience(StringUtils.hasText(dto.getTargetAudience()) ? dto.getTargetAudience().trim() : null)
                .totalDuration(0)
                .learnCount(0)
                .status(CourseStatus.DRAFT.getCode())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        courseMapper.insert(course);
        log.info("管理员创建课程: courseId={}, title={}", course.getId(), course.getTitle());
        return course.getId();
    }

    /**
     * 管理端编辑基本信息：所有状态均可编辑（草稿/待审核/已发布/已驳回/已下架），
     * 编辑后保持原状态不变，不强制重新审核。
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminUpdate(Long courseId, CourseSaveDTO dto) {
        mustGetCourse(courseId);
        validateSaveDTO(dto);
        courseMapper.update(null, new LambdaUpdateWrapper<Course>()
                .eq(Course::getId, courseId)
                .set(Course::getTitle, dto.getTitle().trim())
                .set(Course::getCategory, dto.getCategory())
                .set(Course::getDescription, StringUtils.hasText(dto.getDescription()) ? dto.getDescription().trim() : null)
                .set(Course::getCoverUrl, dto.getCoverUrl())
                .set(Course::getLecturer, StringUtils.hasText(dto.getLecturer()) ? dto.getLecturer().trim() : null)
                .set(Course::getTargetAudience, StringUtils.hasText(dto.getTargetAudience()) ? dto.getTargetAudience().trim() : null)
                .set(Course::getUpdatedAt, LocalDateTime.now()));
        log.info("管理员编辑课程基本信息: courseId={}", courseId);
    }

    /**
     * 提交审核：仅 DRAFT/REJECTED 可提交，转 PENDING 并清驳回原因
     */
    @Transactional(rollbackFor = Exception.class)
    public void submitReview(Long courseId) {
        Course course = mustGetCourse(courseId);
        CourseStatus cur = CourseStatus.fromCode(course.getStatus());
        if (cur != CourseStatus.DRAFT && cur != CourseStatus.REJECTED) {
            throw new BusinessException("仅草稿/已驳回课程可提交审核");
        }
        courseMapper.update(null, new LambdaUpdateWrapper<Course>()
                .eq(Course::getId, courseId)
                .set(Course::getStatus, CourseStatus.PENDING.getCode())
                .set(Course::getRejectReason, null)
                .set(Course::getUpdatedAt, LocalDateTime.now()));
        log.info("课程提交审核: courseId={}", courseId);
    }

    /**
     * 审核：仅 PENDING 可走此接口。approve=true → PUBLISHED + publishedAt；
     * approve=false → REJECTED，必填原因 ≤500
     * <p>审核通过后用户端列表/详情立即可见（用户端查 PUBLISHED 状态）</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void reviewCourse(Long courseId, Boolean approve, String rejectReason) {
        Course course = mustGetCourse(courseId);
        CourseStatus cur = CourseStatus.fromCode(course.getStatus());
        if (cur != CourseStatus.PENDING) {
            throw new BusinessException("仅待审核课程可执行审核操作");
        }
        if (Boolean.TRUE.equals(approve)) {
            courseMapper.update(null, new LambdaUpdateWrapper<Course>()
                    .eq(Course::getId, courseId)
                    .set(Course::getStatus, CourseStatus.PUBLISHED.getCode())
                    .set(Course::getPublishedAt, LocalDateTime.now())
                    .set(Course::getReviewedAt, LocalDateTime.now())
                    .set(Course::getRejectReason, null)
                    .set(Course::getUpdatedAt, LocalDateTime.now()));
            log.info("课程审核通过(已发布): courseId={}", courseId);
        } else {
            if (!StringUtils.hasText(rejectReason)) {
                throw new BusinessException("驳回原因不能为空");
            }
            if (rejectReason.length() > 500) {
                throw new BusinessException("驳回原因不能超过500字");
            }
            courseMapper.update(null, new LambdaUpdateWrapper<Course>()
                    .eq(Course::getId, courseId)
                    .set(Course::getStatus, CourseStatus.REJECTED.getCode())
                    .set(Course::getRejectReason, rejectReason.trim())
                    .set(Course::getReviewedAt, LocalDateTime.now())
                    .set(Course::getUpdatedAt, LocalDateTime.now()));
            log.info("课程审核驳回: courseId={}", courseId);
        }
    }

    /**
     * 上架/下架：仅 PUBLISHED ↔ OFFLINE 互转
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long courseId, Integer targetStatus) {
        Course course = mustGetCourse(courseId);
        CourseStatus cur = CourseStatus.fromCode(course.getStatus());
        CourseStatus target = CourseStatus.fromCode(targetStatus);
        if (target == null || (target != CourseStatus.PUBLISHED && target != CourseStatus.OFFLINE)) {
            throw new BusinessException("目标状态只能为 已发布/已下架");
        }
        if (cur == CourseStatus.PUBLISHED && target == CourseStatus.OFFLINE) {
            courseMapper.update(null, new LambdaUpdateWrapper<Course>()
                    .eq(Course::getId, courseId)
                    .set(Course::getStatus, CourseStatus.OFFLINE.getCode())
                    .set(Course::getUpdatedAt, LocalDateTime.now()));
            log.info("课程下架: courseId={}", courseId);
        } else if (cur == CourseStatus.OFFLINE && target == CourseStatus.PUBLISHED) {
            courseMapper.update(null, new LambdaUpdateWrapper<Course>()
                    .eq(Course::getId, courseId)
                    .set(Course::getStatus, CourseStatus.PUBLISHED.getCode())
                    .set(Course::getUpdatedAt, LocalDateTime.now()));
            log.info("课程上架: courseId={}", courseId);
        } else {
            throw new BusinessException("仅 已发布 ↔ 已下架 可互转");
        }
    }

    // ==================== 管理端：章节 CRUD + 排序 ====================

    /**
     * 新增章节：sortOrder=当前最大值+1，重算 course.totalDuration
     * <p>音频/视频二选一必填其一</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public AdminChapterResponseDTO addChapter(Long courseId, ChapterSaveDTO dto) {
        mustGetCourse(courseId);
        validateChapterDTO(dto);
        Integer maxSort = chapterMapper.selectMaxSortOrder(courseId);
        CourseChapter chapter = CourseChapter.builder()
                .courseId(courseId)
                .title(dto.getTitle().trim())
                .audioUrl(dto.getAudioUrl())
                .videoUrl(dto.getVideoUrl())
                .duration(dto.getDuration() == null ? 0 : dto.getDuration())
                .intro(StringUtils.hasText(dto.getIntro()) ? dto.getIntro() : null)
                .sortOrder((maxSort == null ? 0 : maxSort) + 1)
                .createdAt(LocalDateTime.now())
                .build();
        chapterMapper.insert(chapter);
        recomputeTotalDuration(courseId);
        log.info("新增章节: courseId={}, chapterId={}", courseId, chapter.getId());
        return convert.toAdminChapterDTO(chapter);
    }

    /**
     * 编辑章节：校验属于该课程，重算 course.totalDuration
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateChapter(Long chapterId, ChapterSaveDTO dto) {
        CourseChapter chapter = mustGetChapter(chapterId);
        validateChapterDTO(dto);
        chapterMapper.update(null, new LambdaUpdateWrapper<CourseChapter>()
                .eq(CourseChapter::getId, chapterId)
                .set(CourseChapter::getTitle, dto.getTitle().trim())
                .set(CourseChapter::getAudioUrl, dto.getAudioUrl())
                .set(CourseChapter::getVideoUrl, dto.getVideoUrl())
                .set(CourseChapter::getDuration, dto.getDuration() == null ? 0 : dto.getDuration())
                .set(CourseChapter::getIntro, StringUtils.hasText(dto.getIntro()) ? dto.getIntro() : null));
        recomputeTotalDuration(chapter.getCourseId());
        log.info("编辑章节: chapterId={}, courseId={}", chapterId, chapter.getCourseId());
    }

    /**
     * 删除章节：外键 fk_cp_chapter ON DELETE CASCADE 自动级联清理 course_progress，
     * 重算 course.totalDuration（无孤儿数据）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteChapter(Long chapterId) {
        CourseChapter chapter = mustGetChapter(chapterId);
        Long courseId = chapter.getCourseId();
        chapterMapper.deleteById(chapterId);
        // ponytail: course_progress.chapter_id 外键 ON DELETE CASCADE，DB 自动级联清理，
        // 无需手动删除进度；如未建外键级联则需先 delete progress 再 delete chapter
        recomputeTotalDuration(courseId);
        log.info("删除章节: chapterId={}, courseId={}", chapterId, courseId);
    }

    /**
     * 批量重排：单事务，一次性更新整课 sortOrder
     */
    @Transactional(rollbackFor = Exception.class)
    public void sortChapters(ChapterSortDTO dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            return;
        }
        for (ChapterSortDTO.SortItem item : dto.getItems()) {
            if (item.getId() == null || item.getSortOrder() == null) {
                throw new BusinessException("排序列表项 id/sortOrder 不能为空");
            }
            chapterMapper.updateSortOrder(item.getId(), item.getSortOrder());
        }
        // 排序不影响时长，无需重算
        log.info("批量重排章节: count={}", dto.getItems().size());
    }

    // ==================== 管理端：学习数据 ====================

    /**
     * 课程学习数据：学习人数 / 完成率 / 平均学习时长 / 章节热力图
     * <p>所有指标单 SQL 聚合，禁止逐章逐用户循环</p>
     * <p>兜底同步：查询时重算 course.total_duration，保证存量数据正确</p>
     */
    public CourseStatsResponseDTO getCourseStats(Long courseId) {
        mustGetCourse(courseId);
        // 兜底同步总时长：SUM(章节duration) → course.total_duration
        recomputeTotalDuration(courseId);

        List<CourseChapter> chapters = chapterMapper.selectByCourseId(courseId);
        Integer totalChapterCnt = chapters.size();

        // 学习人数：有任意章节进度记录的去重用户数（对应「正在学习」中有该课程的用户）
        Integer learnerCountRaw = progressMapper.selectLearnerCount(courseId);
        int learnerCount = learnerCountRaw == null ? 0 : learnerCountRaw;

        Integer completedUser = totalChapterCnt > 0
                ? progressMapper.selectCompletedUserCount(courseId, totalChapterCnt) : 0;
        double completionRate = learnerCount > 0
                ? Math.round(completedUser * 1000.0 / learnerCount) / 10.0 : 0.0;

        Integer avgProgress = progressMapper.selectAvgProgressSeconds(courseId);

        // 章节热力图：单 SQL JOIN 聚合
        List<Map<String, Object>> rows = progressMapper.selectChapterHeatmap(courseId);
        // 按 chapters 顺序补齐无学习记录的章节
        Map<Long, Map<String, Object>> rowMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object idObj = row.get("chapter_id");
            if (idObj != null) {
                rowMap.put(((Number) idObj).longValue(), row);
            }
        }
        List<CourseStatsResponseDTO.ChapterHeatmapItem> heatmap = new ArrayList<>();
        for (CourseChapter ch : chapters) {
            Map<String, Object> row = rowMap.get(ch.getId());
            int chLearners = row != null ? ((Number) row.get("learner_count")).intValue() : 0;
            int chFinish = row != null ? ((Number) row.get("finish_count")).intValue() : 0;
            int chAvg = row != null ? (int) Math.round(((Number) row.get("avg_progress")).doubleValue()) : 0;
            heatmap.add(CourseStatsResponseDTO.ChapterHeatmapItem.builder()
                    .chapterId(ch.getId())
                    .title(ch.getTitle())
                    .sortOrder(ch.getSortOrder())
                    .learnerCount(chLearners)
                    .finishCount(chFinish)
                    .avgProgress(chAvg)
                    .build());
        }

        return CourseStatsResponseDTO.builder()
                .learnerCount(learnerCount)
                .completionRate(completionRate)
                .avgProgressSeconds(avgProgress == null ? 0 : avgProgress)
                .chapterHeatmap(heatmap)
                .build();
    }

    // ==================== 管理端：评论管理 ====================

    /**
     * 管理端评论分页：keyword 匹配评论内容（中缀 LIKE），命中 idx_course_created
     */
    public Map<String, Object> adminListComments(Long courseId, String keyword,
                                                long page, long size) {
        long cur = Math.max(page, 1L);
        long sz = Math.min(Math.max(size, 1L), 20L);
        Page<AdminCourseCommentResponseDTO> pageParam = new Page<>(cur, sz);
        IPage<AdminCourseCommentResponseDTO> result = commentMapper.selectAdminPage(
                pageParam, courseId, trimKw(keyword));
        List<AdminCourseCommentResponseDTO> records = result.getRecords().stream()
                .peek(dto -> dto.setHasReply(StringUtils.hasText(dto.getReplyContent())))
                .toList();
        return Map.of("records", records, "total", result.getTotal());
    }

    /**
     * 管理员回复评论：单级，覆盖式更新 reply_content + reply_at
     * <p>管理员回复仍过敏感词过滤（防误操作 + 统一口径）</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void replyComment(Long commentId, String content) {
        CourseComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        if (!StringUtils.hasText(content)) {
            throw new BusinessException("回复内容不能为空");
        }
        if (content.length() > 500) {
            throw new BusinessException("回复内容不能超过500字");
        }
        sensitiveWordFilter.checkSensitive(content);
        commentMapper.update(null, new LambdaUpdateWrapper<CourseComment>()
                .eq(CourseComment::getId, commentId)
                .set(CourseComment::getReplyContent, content.trim())
                .set(CourseComment::getReplyAt, LocalDateTime.now()));
        log.info("管理员回复评论: commentId={}, courseId={}", commentId, comment.getCourseId());
    }

    /**
     * 删除评论：物理删（前台若有回复一并删除，因为回复挂在评论行上）
     */
    @Transactional(rollbackFor = Exception.class)
    public void adminDeleteComment(Long commentId) {
        CourseComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        commentMapper.deleteById(commentId);
        log.info("管理员删除评论: commentId={}, courseId={}", commentId, comment.getCourseId());
    }

    // ==================== 管理端：内部工具方法 ====================

    private Course mustGetCourse(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        return course;
    }

    private CourseChapter mustGetChapter(Long chapterId) {
        CourseChapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new BusinessException("章节不存在");
        }
        return chapter;
    }

    /**
     * 课程基本信息保存 DTO 校验：类别合法、URL 长度限制
     */
    private void validateSaveDTO(CourseSaveDTO dto) {
        if (CourseCategory.fromCode(dto.getCategory()) == null) {
            throw new BusinessException("课程类别不存在");
        }
        if (dto.getCoverUrl() != null && dto.getCoverUrl().length() > 500) {
            throw new BusinessException("封面URL不能超过500字");
        }
    }

    /**
     * 章节 DTO 校验：音频/视频二选一必填其一（OSS 上传产物 URL）
     */
    private void validateChapterDTO(ChapterSaveDTO dto) {
        boolean hasAudio = StringUtils.hasText(dto.getAudioUrl());
        boolean hasVideo = StringUtils.hasText(dto.getVideoUrl());
        if (!hasAudio && !hasVideo) {
            throw new BusinessException("音频或视频地址至少填写一项");
        }
        if (dto.getDuration() != null && dto.getDuration() < 0) {
            throw new BusinessException("时长不能为负数");
        }
    }

    /**
     * 重算课程总时长：SUM(chapter.duration) → update course.total_duration
     * <p>章节增/删/改在 @Transactional 内调用，保证一致性</p>
     */
    private void recomputeTotalDuration(Long courseId) {
        Integer sum = chapterMapper.selectTotalDuration(courseId);
        courseMapper.update(null, new LambdaUpdateWrapper<Course>()
                .eq(Course::getId, courseId)
                .set(Course::getTotalDuration, sum == null ? 0 : sum)
                .set(Course::getUpdatedAt, LocalDateTime.now()));
    }

    private String trimKw(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }
}
