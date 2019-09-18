package curator;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.BackgroundCallback;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import java.util.concurrent.Executors;

/**
 * @author admin
 * @title: CuratorBaseTest
 * @projectName zookeeper
 * @description: TODO
 * @date 2019/9/1816:15
 */
public class CuratorBaseTest {

    private static final String zkServerUrl = "127.0.0.1:12181,127.0.0.1:12182,127.0.0.1:12183";

    private static RetryPolicy retryPolicy  = new ExponentialBackoffRetry(1000,3);
    private static CuratorFramework client = CuratorFrameworkFactory.builder()
            .connectString(zkServerUrl)
            .sessionTimeoutMs(60000)
            .connectionTimeoutMs(15000)
            .retryPolicy(retryPolicy)
            .build();

    public static void main(String[] args) {
        /**
         * 创建会话
         * */
        client.start();

        try {
            /**
             * 同步创建节点
             * 注意：
             * 1.除非指明创建节点的类型,默认是持久节点
             * 2.ZooKeeper规定:所有非叶子节点都是持久节点,所以递归创建出来的节点,
             *   只有最后的数据节点才是指定类型的节点,其父节点是持久节点
             * */

//            //创建一个初始内容为空的节点
//            client.create().forPath("/China");
//            //创建一个初始内容不为空的节点
//            client.create().forPath("/Korea","tsfeng".getBytes());
//            //创建一个初始内容为空的临时节点
//            client.create().withMode(CreateMode.EPHEMERAL).forPath("/America");
//            //创建一个初始内容不为空的临时节点，可以实现递归创建
//            client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL)
//                    .forPath("/Japan", "tsfeng".getBytes());


            /**
             *  异步创建节点
             *
             * 注意:如果自己指定了线程池,那么相应的操作就会在线程池中执行,如果没有指定,
             *   那么就会使用Zookeeper的EventThread线程对事件进行串行处理
             * */
            client.create().withMode(CreateMode.EPHEMERAL).inBackground(new BackgroundCallback() {
                @Override
                public void processResult(CuratorFramework client, CuratorEvent event) throws Exception {
                    System.out.println("当前线程：" + Thread.currentThread().getName() + ",code:"
                            + event.getResultCode() + ",type:" + event.getType());
                }
            }, Executors.newFixedThreadPool(10)).forPath("/async-China");


            client.create().withMode(CreateMode.EPHEMERAL).inBackground(new BackgroundCallback() {
                public void processResult(CuratorFramework client, CuratorEvent event) throws Exception {
                    System.out.println("当前线程：" + Thread.currentThread().getName() + ",code:"
                            + event.getResultCode() + ",type:" + event.getType());
                }
            }).forPath("/async-America");

            /**
             * 获取节点内容
             * */
            byte[] data = client.getData().forPath("/Korea");
            System.out.println(new String(data));
            //传入一个旧的stat变量,来存储服务端返回的最新的节点状态信息
            byte[] data2 = client.getData().storingStatIn(new Stat()).forPath("/Korea");
            System.out.println(new String(data2));

            /**
             * 更新数据
             * */
//            Stat stat = client.setData().forPath("/Korea");
//            client.setData().withVersion(4).forPath("/Korea", "tsfeng".getBytes());

            /**
             * 删除节点
             * */
//            //只能删除叶子节点
//            client.delete().forPath("/China");
//            //删除一个节点,并递归删除其所有子节点
//            client.delete().deletingChildrenIfNeeded().forPath("/aa");
//            //强制指定版本进行删除
//            client.delete().withVersion(4).forPath("/Korea");
            //注意:由于一些网络原因,上述的删除操作有可能失败,使用guaranteed(),
            // 如果删除失败,会记录下来,只要会话有效,就会不断的重试,直到删除成功为止
//            client.delete().guaranteed().forPath("/America");


//            Thread.sleep(Integer.MAX_VALUE);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
