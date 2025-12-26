import javax.swing.SwingUtilities;
import controller.PublicTransportSystem;  // 添加这一行

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PublicTransportSystem system = new PublicTransportSystem();
            system.setVisible(true);
        });
    }
}