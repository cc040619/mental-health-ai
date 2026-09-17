package com.cc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.dto.request.NotifySettingUpdateDTO;
import com.cc.dto.response.NotifySettingResponseDTO;
import com.cc.entity.UserNotifySetting;
import com.cc.enums.AssessmentFrequency;
import com.cc.exception.BusinessException;
import com.cc.mapper.UserNotifySettingMapper;
import com.cc.util.CurrentUserUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户消息设置业务层：无记录时返回默认值并落一条默认设置
 *
 * @author cc
 * @date 2026-09-14
 */
@Slf4j
@Service
public class NotifySettingService {

    @Resource
    private UserNotifySettingMapper settingMapper;

    /**
     * 获取当前用户消息设置（无记录则创建默认：4 开关全开 + 每周提醒）
     */
    public NotifySettingResponseDTO getSettings() {
        return toDTO(getOrCreate(CurrentUserUtils.getCurrentUserId()));
    }

    /**
     * 更新当前用户消息设置
     */
    public NotifySettingResponseDTO updateSettings(NotifySettingUpdateDTO dto) {
        if (AssessmentFrequency.fromCode(dto.getAssessmentFrequency()) == null) {
            throw new BusinessException("测评提醒频率非法");
        }
        Long userId = CurrentUserUtils.getCurrentUserId();
        UserNotifySetting setting = getOrCreate(userId);
        setting.setAnnouncementOn(dto.getAnnouncementOn());
        setting.setAppointmentOn(dto.getAppointmentOn());
        setting.setInteractionOn(dto.getInteractionOn());
        setting.setAssessmentOn(dto.getAssessmentOn());
        setting.setConsultantOn(dto.getConsultantOn());
        setting.setAssessmentFrequency(dto.getAssessmentFrequency());
        settingMapper.updateById(setting);
        log.info("更新消息设置: userId={}", userId);
        return toDTO(setting);
    }

    /** 查设置，不存在则插入默认记录 */
    private UserNotifySetting getOrCreate(Long userId) {
        UserNotifySetting setting = settingMapper.selectOne(new LambdaQueryWrapper<UserNotifySetting>()
                .eq(UserNotifySetting::getUserId, userId));
        if (setting != null) {
            return setting;
        }
        setting = UserNotifySetting.builder()
                .userId(userId)
                .announcementOn(1)
                .appointmentOn(1)
                .interactionOn(1)
                .assessmentOn(1)
                .consultantOn(1)
                .assessmentFrequency(AssessmentFrequency.WEEKLY.getCode())
                .build();
        settingMapper.insert(setting);
        return setting;
    }

    private NotifySettingResponseDTO toDTO(UserNotifySetting s) {
        NotifySettingResponseDTO dto = new NotifySettingResponseDTO();
        dto.setAnnouncementOn(s.getAnnouncementOn());
        dto.setAppointmentOn(s.getAppointmentOn());
        dto.setInteractionOn(s.getInteractionOn());
        dto.setAssessmentOn(s.getAssessmentOn());
        dto.setConsultantOn(s.getConsultantOn());
        dto.setAssessmentFrequency(s.getAssessmentFrequency());
        return dto;
    }
}
