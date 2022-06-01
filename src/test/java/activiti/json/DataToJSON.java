package activiti.json;

/**
 * @author 仙晓明
 * @date 2022/5/31 21:23
 */
public class DataToJSON {
    public static void main(String[] args) {

        String s=":{operateDetail=completeTask, parentTaskId=, processorOperateId=19630, workOrderId=1094285, taskId=20802167, taskDefId=sid-A538DB04-3F31-44A3-BF55-B1B73698DA70, md5=70cdffa3e66f8c641e9cab9f6ff951bc}";


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
