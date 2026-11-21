package com.educenter.dto;

import com.educenter.entity.StudentAIStudyPlanItem;
import java.util.List;

public record StudentProgressOverview(int scoredAnswerCount, int publishedExamCount, boolean enoughTopicData,
                                      List<StudentTopicProgress> topics, List<StudentAIStudyPlanItem> studyPlan,
                                      boolean studyPlanRefreshNeeded, String studyPlanMessage) { }
