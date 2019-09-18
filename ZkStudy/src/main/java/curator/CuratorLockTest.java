package curator;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * FakeLimitedResource
 * @author admin
 * @title: CuratorLockTest
 * @projectName zookeeper
 * @description: TODO
 * @date 2019/9/1815:01
 */
public class CuratorLockTest {

    private static final String zkServerUrl = "127.0.0.1:12181,127.0.0.1:12182,127.0.0.1:12183";

    private static final String lockPath = "/curator/lock";

    private String machineName;

    public CuratorLockTest(String machineName) {
        this.machineName = machineName;
    }

    public void getLock() {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFramework client = CuratorFrameworkFactory.newClient(zkServerUrl, retryPolicy);
        client.start();
        try {
            System.out.println(machineName + "zooKeeper 建立连接成功" + client.getZookeeperClient().getZooKeeper());
            // 创建分布式锁, 锁空间的根节点路径为  /curator/lock
            // 一种可以跨 JVM 工作的可重入互斥锁。
            // 使用 Zookeeper 来保持锁定。所有使用相同锁定路径的 JVM 中的所有进程都将实现进程间关键部分。
            // 此外，此互斥锁是“公平的” - 每个用户将按请求的顺序获取互斥锁（从ZK的角度来看）
            InterProcessMutex lock = new InterProcessMutex(client, lockPath);
            if (lock.acquire(1, TimeUnit.SECONDS)) {
                try {
                    System.out.println(machineName + " 获得锁");
                    TimeUnit.SECONDS.sleep(3);
                } finally {
                    //完成业务流程, 释放锁
                    System.out.println(machineName + " 释放锁");
                    lock.release();
                }
            } else {
                System.out.println(machineName + " 获取锁失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        IntStream.rangeClosed(1, 5)
                .mapToObj(index -> "机器" + index)
                .map(CuratorLockTest::new)
                .map(lockTest -> (Runnable) () -> lockTest.getLock())
                .map(Thread::new)
                .forEach(Thread::start);
        System.out.println("over");
        TimeUnit.SECONDS.sleep(16);
    }
}
