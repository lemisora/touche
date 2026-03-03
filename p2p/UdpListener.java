import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UdpListener {
    public static void main(String[] args) {
        try {
            DatagramSocket socket = new DatagramSocket(8888);
            byte[] buf = new byte[1024];
            System.out.println("Listening on 8888...");
            while (true) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received from " + packet.getAddress().getHostAddress() + " : " + msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
