package activiti.model;

import lombok.Data;

import java.io.Serializable;

/**
 * @author 仙晓明
 * @date 2022/6/2 11:07
 */
@Data
public class FormProperty implements Serializable {
    private static final long serialVersionUID = -3916983500640326345L;

    private String id;
    private String name;
    private String variableExpression;
    private String variableName;

}
