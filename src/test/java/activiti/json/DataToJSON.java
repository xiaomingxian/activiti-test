package activiti.json;

/**
 * @author 仙晓明
 * @date 2022/5/31 21:23
 */
public class DataToJSON {
    public static void main(String[] args) {

        String s="{operateDetail=completeTask, parentTaskId=, processorOperateId=19630, workOrderId=1097001, taskId=20863350, taskDefId=sid-4836C963-187E-4F55-835C-E02FBA87E5FC, md5=182e64271a7a0342f9f6c68992ad3e38}";

        String s1 = s.replaceAll("=", "\":\"")
                .replaceAll("\\{","{\"")
                .replaceAll("}","\"}")
                .replaceAll(", ","\",\"");

        System.out.println();
        System.out.println(s1);
        System.out.println();

    }
}
