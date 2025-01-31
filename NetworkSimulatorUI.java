import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class NetworkSimulatorUI extends JFrame {
    private NetworkSimulator simulator = new NetworkSimulator();
    private JTextArea logArea;
    private JPanel canvasPanel;
    private Map<String, Point> deviceLocations = new HashMap<>(); // Store device locations for visualization
    private List<RouteVisualization> routes = new ArrayList<>(); // Store routes for visualization
    private String selectedTopology = "Default"; // Default topology
     // Current packet location for visualization

    public NetworkSimulatorUI() {
        setTitle("Network Simulator UI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLayout(new BorderLayout());

        // Topology Selection
        String[] topologies = {"Default", "Bus", "Star", "Ring", "Mesh"};
        selectedTopology = (String) JOptionPane.showInputDialog(
            this, "Select Network Topology:", "Topology Selection",
            JOptionPane.QUESTION_MESSAGE, null, topologies, topologies[0]);

        if (selectedTopology == null) selectedTopology = "Default"; // Default if cancelled

        // Canvas for drawing network topology
        canvasPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawNetworkTopology(g);
                
            }
        };
        canvasPanel.setBackground(Color.WHITE);
        add(canvasPanel, BorderLayout.CENTER);

        // Control Panel
        JPanel controlPanel = new JPanel(new GridLayout(1, 4));
        JButton addDeviceButton = new JButton("Add Device");
        JButton addRouteButton = new JButton("Add Route");
        JButton startSimulationButton = new JButton("Start Simulation");
        JButton stopSimulationButton = new JButton("Stop Simulation");

        controlPanel.add(addDeviceButton);
        controlPanel.add(addRouteButton);
        controlPanel.add(startSimulationButton);
        controlPanel.add(stopSimulationButton);
        add(controlPanel, BorderLayout.NORTH);

        addDeviceButton.addActionListener(e -> showAddDeviceDialog());
        addRouteButton.addActionListener(e -> showAddRouteDialog());
        startSimulationButton.addActionListener(e -> startSimulation());
        stopSimulationButton.addActionListener(e -> stopSimulation());

        // Log Area
        logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.SOUTH);

        setVisible(true);
    }

    public void log(String message) {
        logArea.append(message + "\n");
    }

    /*private void drawLegend(Graphics g) {
        g.setColor(Color.BLACK);
        g.drawString("Legend:", 10, canvasPanel.getHeight() - 50);
        g.setColor(Color.GREEN);
        g.fillOval(10, canvasPanel.getHeight() - 40, 10, 10);
        g.drawString("Packet Movement", 30, canvasPanel.getHeight() - 30);
        g.setColor(Color.RED);
        g.fillRect(10, canvasPanel.getHeight() - 20, 10, 10);
        g.drawString("Packet Drop", 30, canvasPanel.getHeight() - 10);
    }*/

    private void showAddDeviceDialog() {
        JDialog dialog = new JDialog(this, "Add Device", true);
        dialog.setSize(400, 200);
        dialog.setLayout(new GridLayout(4, 2));

        JTextField deviceNameField = new JTextField();
        JTextField ipField = new JTextField();
        JTextField subnetMaskField = new JTextField();
        JButton addButton = new JButton("Add");
        JButton cancelButton = new JButton("Cancel");

        dialog.add(new JLabel("Device Name:"));
        dialog.add(deviceNameField);
        dialog.add(new JLabel("IP Address:"));
        dialog.add(ipField);
        dialog.add(new JLabel("Subnet Mask:"));
        dialog.add(subnetMaskField);
        dialog.add(addButton);
        dialog.add(cancelButton);

        addButton.addActionListener(e -> {
            String deviceName = deviceNameField.getText().trim();
            String ipAddress = ipField.getText().trim();
            String subnetMask = subnetMaskField.getText().trim();

            if (deviceName.isEmpty() || ipAddress.isEmpty() || subnetMask.isEmpty()) {
                log("Error: All fields are required!");
                return;
            }

            if (!isValidIP(ipAddress) || !isValidSubnetMask(subnetMask)) {
                log("Error: Invalid IP Address or Subnet Mask format!");
                return;
            }

            simulator.addDevice(deviceName, ipAddress, subnetMask);
            addDeviceToCanvas(deviceName);
            canvasPanel.repaint();
            log("Device added: " + deviceName + " (" + ipAddress + "/" + subnetMask + ")");
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }

    private void showAddRouteDialog() {
        JDialog dialog = new JDialog(this, "Add Route", true);
        dialog.setSize(400, 200);
        dialog.setLayout(new GridLayout(4, 2));

        JComboBox<String> sourceComboBox = new JComboBox<>(simulator.getDevices().keySet().toArray(new String[0]));
        JTextField destinationIpField = new JTextField();
        JTextField nextHopField = new JTextField();
        JButton addButton = new JButton("Add");
        JButton cancelButton = new JButton("Cancel");

        dialog.add(new JLabel("Source Device:"));
        dialog.add(sourceComboBox);
        dialog.add(new JLabel("Destination IP Address:"));
        dialog.add(destinationIpField);
        dialog.add(new JLabel("Next Hop Device:"));
        dialog.add(nextHopField);
        dialog.add(addButton);
        dialog.add(cancelButton);

        addButton.addActionListener(e -> {
            String sourceDevice = (String) sourceComboBox.getSelectedItem();
            String destinationIp = destinationIpField.getText().trim();
            String nextHop = nextHopField.getText().trim();

            if (sourceDevice == null || destinationIp.isEmpty() || nextHop.isEmpty()) {
                log("Error: All fields are required!");
                return;
            }

            simulator.addStaticRoute(sourceDevice, destinationIp, nextHop);
            addRouteToCanvas(sourceDevice, nextHop);
            canvasPanel.repaint();
            log("Route added: " + sourceDevice + " -> " + nextHop + " (via " + destinationIp + ")");
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }

    private void addDeviceToCanvas(String deviceName) {
        int x = 0, y = 0;
        int deviceCount = deviceLocations.size();
        switch (selectedTopology) {
            case "Bus":
                x = 100 + deviceCount * 150;
                y = 300;
                break;
            case "Star":
                x = 400 + (int) (200 * Math.cos(2 * Math.PI * deviceCount / 6));
                y = 300 + (int) (200 * Math.sin(2 * Math.PI * deviceCount / 6));
                break;
            case "Ring":
                x = 500 + (int) (200 * Math.cos(2 * Math.PI * deviceCount / 6));
                y = 300 + (int) (200 * Math.sin(2 * Math.PI * deviceCount / 6));
                break;
            case "Mesh":
                x = 100 + (deviceCount % 4) * 200;
                y = 100 + (deviceCount / 4) * 200;
                break;
            default: // Default Horizontal Layout
                x = 100 + deviceCount * 150;
                y = 300;
        }
        deviceLocations.put(deviceName, new Point(x, y));
    }

    private void addRouteToCanvas(String sourceDevice, String nextHop) {
        Point sourcePoint = deviceLocations.get(sourceDevice);
        Point nextHopPoint = deviceLocations.get(nextHop);
        if (sourcePoint != null && nextHopPoint != null) {
            routes.add(new RouteVisualization(sourcePoint, nextHopPoint));
        }
    }

    private void drawNetworkTopology(Graphics g) {
        // Draw routes
        g.setColor(Color.GREEN);
        for (RouteVisualization route : routes) {
            g.drawLine(route.source.x, route.source.y, route.destination.x, route.destination.y);
        }

        // Draw devices
        for (Map.Entry<String, Point> entry : deviceLocations.entrySet()) {
            String deviceName = entry.getKey();
            Point location = entry.getValue();
            boolean isRouter = deviceName.toLowerCase().contains("router");

            g.setColor(isRouter ? Color.BLUE : Color.BLACK);
            if (isRouter) {
                g.drawOval(location.x, location.y, 50, 50);
            } else {
                g.drawRect(location.x, location.y, 50, 50);
            }

            g.setColor(Color.BLACK);
            g.drawString(deviceName, location.x + 5, location.y - 5);
        }
    }
    
    private void animatePacketMovement(List<Point> path) {
        Graphics g = canvasPanel.getGraphics();
      
        for (int i = 0; i < path.size() - 1; i++) {
          Point start = path.get(i);
          Point end = path.get(i + 1);
      
          int steps = 50; // Increase for smoother movement
          for (int step = 0; step <= steps; step++) {
            int x = start.x + (end.x - start.x) * step / steps;
            int y = start.y + (end.y - start.y) * step / steps;
      
            // Draw the moving dot without clearing the canvas
            g.setColor(Color.BLUE);
            g.fillOval(x, y, 10, 10); // Example packet size
      
            try {
              Thread.sleep(100); // Adjust delay as needed
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
        }
      }
    /*private void animatePacketMovement(List<Point> path) {
        Graphics g = canvasPanel.getGraphics();
    
        for (int i = 0; i < path.size() - 1; i++) {
            Point start = path.get(i);
            Point end = path.get(i + 1);
    
            int steps = 500; // Increase the number of animation steps for smoother movement
            for (int step = 0; step <= steps; step++) {
                int x = start.x + (end.x - start.x) * step / steps;
                int y = start.y + (end.y - start.y) * step / steps;
    
                // Clear the previous dot and redraw topology
                canvasPanel.repaint();
    
                // Draw the moving dot
                g.setColor(Color.BLUE);
                g.fillOval(x, y, 10, 10);
    
                try {
                    Thread.sleep(5000); // Slow down the animation for better visibility
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }*/
    
    private void startSimulation() {
        String source = JOptionPane.showInputDialog(this, "Enter source device name:");
        String destination = JOptionPane.showInputDialog(this, "Enter destination device name:");
    
        if (source == null || destination == null) {
            log("Simulation cancelled.");
            return;
        }
    
        log("Simulating packet from " + source + " to " + destination);
    
        new Thread(() -> {
            // Get the path from the simulator
            List<String> devicePath = simulator.simulatePacketPath(source, destination);
            List<Point> pointPath = new ArrayList<>();
    
            // Convert device names to points
            for (String device : devicePath) {
                Point location = deviceLocations.get(device);
                if (location != null) {
                    pointPath.add(location);
                }
            }
    
            // Animate the packet movement along the path
            animatePacketMovement(pointPath);
    
            // Final repaint
            canvasPanel.repaint();
            log("Simulation complete from " + source + " to " + destination);
        }).start();
    }
    
    

    private void stopSimulation() {
        simulator.stopSimulation();
        canvasPanel.repaint();
        log("Simulation stopped.");
    }

    private boolean isValidIP(String ipAddress) {
        return ipAddress.matches("(\\d{1,3}\\.){3}\\d{1,3}");
    }

    private boolean isValidSubnetMask(String subnetMask) {
        return subnetMask.matches("(\\d{1,3}\\.){3}\\d{1,3}");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(NetworkSimulatorUI::new);
    }

    private class RouteVisualization {
        Point source, destination;

        RouteVisualization(Point source, Point destination) {
            this.source = source;
            this.destination = destination;
        }
    }
}
