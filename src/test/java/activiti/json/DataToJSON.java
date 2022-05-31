package activiti.json;

/**
 * @author 仙晓明
 * @date 2022/5/31 21:23
 */
public class DataToJSON {
    public static void main(String[] args) {

        String s="{operateDetail=completeTask, parentTaskId=, processorOperateId=19682, workOrderId=1094583, taskId=20807257, taskDefId=sid-A538DB04-3F31-44A3-BF55-B1B73698DA70, md5=1bd0da443975d981e5d46e81b640f8a2}";


        String s1 = s.replaceAll("=", "\":\"")
                .replaceAll("\\{","{\"")
                .replaceAll("}","\"}")
                .replaceAll(", ","\",\"")
                ;

        System.out.println();
        System.out.println(s1);
        System.out.println();

    }
}
