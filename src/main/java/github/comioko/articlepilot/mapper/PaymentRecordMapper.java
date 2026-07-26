package github.comioko.articlepilot.mapper;

import com.mybatisflex.core.BaseMapper;
import github.comioko.articlepilot.model.entity.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付记录 Mapper
 *
 * @author comioko
 */
@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {
}