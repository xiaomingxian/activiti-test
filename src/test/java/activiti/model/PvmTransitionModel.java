package activiti.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 流程图的带条件连线
 *
 * @author 仙晓明
 * @date 2022/5/31 15:37
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PvmTransitionModel implements Serializable {

    /**
     * 连线上的控制条件key
     */
    List<String> keys=new ArrayList<>(0);

    /**
     * 连线后可能存在的网关
     */
    List<PvmTransitionModel> otherLine=new ArrayList<>(0);

}
