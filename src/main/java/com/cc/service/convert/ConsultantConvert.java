package com.cc.service.convert;

import com.cc.dto.request.ConsultantSaveDTO;
import com.cc.dto.response.ConsultantAdminDetailResponseDTO;
import com.cc.dto.response.ConsultantAdminListResponseDTO;
import com.cc.dto.response.ConsultantDetailResponseDTO;
import com.cc.dto.response.ConsultantListResponseDTO;
import com.cc.dto.response.ReviewResponseDTO;
import com.cc.dto.response.SlotResponseDTO;
import com.cc.entity.AppointmentSlot;
import com.cc.entity.Consultant;
import com.cc.entity.ConsultantReview;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * 预约咨询模块转换器：咨询师/时段/评价 Entity → DTO、保存 DTO → Entity。
 * 禁止在 controller/service 手写转换。
 *
 * @author cc
 * @date 2026-09-14
 */
@Component
public class ConsultantConvert {

    /** 时段时间统一输出格式：HH:mm（两位小时，前端可直接比对） */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public ConsultantListResponseDTO toListDTO(Consultant c) {
        if (c == null) {
            return null;
        }
        ConsultantListResponseDTO dto = new ConsultantListResponseDTO();
        fillCard(c, dto);
        return dto;
    }

    /** 管理端列表行：卡片字段 + 审核状态字段 */
    public ConsultantAdminListResponseDTO toAdminListDTO(Consultant c) {
        if (c == null) {
            return null;
        }
        ConsultantAdminListResponseDTO dto = new ConsultantAdminListResponseDTO();
        fillCard(c, dto);
        dto.setStatus(c.getStatus());
        dto.setRejectReason(c.getRejectReason());
        dto.setReviewedAt(c.getReviewedAt());
        dto.setJoinedAt(c.getJoinedAt());
        return dto;
    }

    public ConsultantDetailResponseDTO toDetailDTO(Consultant c) {
        if (c == null) {
            return null;
        }
        ConsultantDetailResponseDTO dto = new ConsultantDetailResponseDTO();
        fillCard(c, dto);
        dto.setIntro(c.getIntro());
        dto.setBookingWindowDays(c.getBookingWindowDays());
        return dto;
    }

    /** 管理端详情：含资质/审核/排序/价格，仅管理员接口使用 */
    public ConsultantAdminDetailResponseDTO toAdminDetailDTO(Consultant c) {
        if (c == null) {
            return null;
        }
        ConsultantAdminDetailResponseDTO dto = new ConsultantAdminDetailResponseDTO();
        fillCard(c, dto);
        dto.setIntro(c.getIntro());
        dto.setStatus(c.getStatus());
        dto.setQualificationImages(c.getQualificationImages());
        dto.setRejectReason(c.getRejectReason());
        dto.setReviewedAt(c.getReviewedAt());
        dto.setJoinedAt(c.getJoinedAt());
        dto.setSortWeight(c.getSortWeight());
        dto.setPrice(c.getPrice());
        return dto;
    }

    public SlotResponseDTO toSlotDTO(AppointmentSlot slot) {
        if (slot == null) {
            return null;
        }
        SlotResponseDTO dto = new SlotResponseDTO();
        dto.setId(slot.getId());
        dto.setSlotDate(slot.getSlotDate().format(DATE_FORMATTER));
        dto.setStartTime(slot.getStartTime().format(TIME_FORMATTER));
        dto.setEndTime(slot.getEndTime().format(TIME_FORMATTER));
        dto.setStatus(slot.getStatus());
        return dto;
    }

    public ReviewResponseDTO toReviewDTO(ConsultantReview review) {
        ReviewResponseDTO dto = new ReviewResponseDTO();
        dto.setId(review.getId());
        dto.setRating(review.getRating());
        dto.setContent(review.getContent());
        // 匿名展示，不泄露评价人身份
        dto.setReviewerName("来访者");
        dto.setReplyContent(review.getReplyContent());
        dto.setReplyAt(review.getReplyAt());
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }

    /** 新增：保存 DTO → Entity（统计/审核字段走 DB 默认值，不信任前端） */
    public Consultant toEntity(ConsultantSaveDTO dto) {
        return Consultant.builder()
                .name(dto.getName().trim())
                .avatar(trimToNull(dto.getAvatar()))
                .title(trimToNull(dto.getTitle()))
                .specialties(trimToNull(dto.getSpecialties()))
                .yearsOfPractice(dto.getYearsOfPractice())
                .intro(trimToNull(dto.getIntro()))
                .qualificationImages(dto.getQualificationImages())
                .sortWeight(dto.getSortWeight() == null ? 0 : dto.getSortWeight())
                .price(dto.getPrice())
                .build();
    }

    /** 编辑：仅覆盖可编辑字段，统计字段/审核状态不由此 DTO 改动 */
    public void mergeUpdate(Consultant entity, ConsultantSaveDTO dto) {
        entity.setName(dto.getName().trim());
        entity.setAvatar(trimToNull(dto.getAvatar()));
        entity.setTitle(trimToNull(dto.getTitle()));
        entity.setSpecialties(trimToNull(dto.getSpecialties()));
        entity.setYearsOfPractice(dto.getYearsOfPractice());
        entity.setIntro(trimToNull(dto.getIntro()));
        entity.setQualificationImages(dto.getQualificationImages());
        entity.setSortWeight(dto.getSortWeight() == null ? 0 : dto.getSortWeight());
        entity.setPrice(dto.getPrice());
    }

    /** 列表/详情卡片公共字段 */
    private void fillCard(Consultant c, ConsultantListResponseDTO dto) {
        dto.setId(c.getId());
        dto.setName(c.getName());
        dto.setAvatar(c.getAvatar());
        dto.setTitle(c.getTitle());
        dto.setSpecialties(c.getSpecialties());
        dto.setYearsOfPractice(c.getYearsOfPractice());
        dto.setRating(c.getRating());
        dto.setReviewCount(c.getReviewCount());
        dto.setConsultCount(c.getConsultCount());
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
