package github.comioko.articlepilot.service;


import github.comioko.articlepilot.model.vo.StatisticsVO;

/**
 * 统计服务
 *
 * @author comioko
 */
public interface StatisticsService {

    /**
     * 获取系统统计数据
     *
     * @return 统计数据
     */
    StatisticsVO getStatistics();
}