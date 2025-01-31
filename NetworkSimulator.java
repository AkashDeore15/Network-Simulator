import java.util.*;

public class NetworkSimulator {
    private Map<String, NetworkDevice> devices = new HashMap<>();
    private List<Route> routes = new ArrayList<>();
    private boolean isSimulating = false;
    private int totalPacketsForwarded = 0;
    private int totalPacketsLost = 0;
    private int delayThreshold = 500; // 500ms threshold for packet drop

    public void addDevice(String name, String ipAddress, String subnetMask) {
        if (devices.containsKey(name) || devices.values().stream().anyMatch(d -> d.getIpAddress().equals(ipAddress))) {
            System.out.println("Error: Device name or IP address already exists.");
            return;
        }
        NetworkDevice device = new NetworkDevice(name, ipAddress, subnetMask);
        devices.put(name, device);
    }

    public void addStaticRoute(String sourceDevice, String destinationIpWithMask, String nextHop) {
        if (destinationIpWithMask.contains("/")) {
            // CIDR validation
            String[] parts = destinationIpWithMask.split("/");
            if (parts.length != 2) {
                System.out.println("Invalid CIDR notation: " + destinationIpWithMask);
                return;
            }

            String destinationIp = parts[0];
            int prefixLength;
            try {
                prefixLength = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid CIDR prefix: " + destinationIpWithMask);
                return;
            }

            NetworkDevice device = devices.get(sourceDevice);
            if (device != null) {
                device.addRoute(new RouteEntry(destinationIp, prefixLength, nextHop));
            } else {
                System.out.println("Error: Source device " + sourceDevice + " not found.");
            }
        } else {
            // Single IP handling
            if (!isValidIP(destinationIpWithMask)) {
                System.out.println("Invalid IP address: " + destinationIpWithMask);
                return;
            }

            NetworkDevice device = devices.get(sourceDevice);
            if (device != null) {
                device.addRoute(new RouteEntry(destinationIpWithMask, 32, nextHop)); // /32 for single IP
            } else {
                System.out.println("Error: Source device " + sourceDevice + " not found.");
            }
        }
    }
    public List<String> simulatePacketPath(String sourceDevice, String destinationDevice) {
        List<String> path = new ArrayList<>();
        NetworkDevice source = devices.get(sourceDevice);
        NetworkDevice destination = devices.get(destinationDevice);
    
        if (source == null || destination == null) {
            System.out.println("Error: Invalid source or destination device.");
            return path;
        }
    
        System.out.println("Simulating packet from " + sourceDevice + " to " + destinationDevice);
    
        String currentDevice = sourceDevice;
        while (!currentDevice.equals(destinationDevice)) {
            NetworkDevice current = devices.get(currentDevice);
            if (current == null) {
                System.out.println("Error: Device " + currentDevice + " not found.");
                break;
            }
    
            path.add(currentDevice); // Add current device to path
    
            // Find the next-hop using longest prefix match
            String nextHop = current.findNextHop(destination.getIpAddress());
            if (nextHop == null) {
                System.out.println("No route to destination from " + currentDevice);
                break;
            }
    
            System.out.println("Packet forwarded from " + currentDevice + " to " + nextHop);
            currentDevice = nextHop;
            
            try {
                Thread.sleep(100); // Simulate transmission delay
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
    
            if (System.currentTimeMillis() % delayThreshold == 0) {
                System.out.println("Packet dropped due to delay.");
                break;
            }
        }
    
        if (currentDevice.equals(destinationDevice)) {
            path.add(destinationDevice); // Add destination to path
            System.out.println("Packet successfully delivered to " + destinationDevice);
        }
    
        return path;
    }
    

    private boolean isInSubnet(String ipAddress, String subnetIp, int prefixLength) {
        int ip = toInt(ipAddress);
        int subnet = toInt(subnetIp);
        int mask = ~((1 << (32 - prefixLength)) - 1);
        return (ip & mask) == (subnet & mask);
    }

    private int toInt(String ipAddress) {
        String[] parts = ipAddress.split("\\.");
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result |= (Integer.parseInt(parts[i]) << (24 - (i * 8)));
        }
        return result;
    }

    private boolean isValidIP(String ipAddress) {
        return ipAddress.matches("(\\d{1,3}\\.){3}\\d{1,3}");
    }

    public void simulatePacketForwarding(String sourceDevice, String destinationDevice) {
        NetworkDevice source = devices.get(sourceDevice);
        NetworkDevice destination = devices.get(destinationDevice);
    
        if (source == null || destination == null) {
            System.out.println("Error: Invalid source or destination device.");
            return;
        }
    
        System.out.println("Simulating packet from " + sourceDevice + " to " + destinationDevice);
    
        String currentDevice = sourceDevice;
        while (!currentDevice.equals(destinationDevice)) {
            NetworkDevice current = devices.get(currentDevice);
            if (current == null) {
                System.out.println("Error: Device " + currentDevice + " not found.");
                break;
            }
    
            // Find the next-hop using longest prefix match
            String nextHop = current.findNextHop(destination.getIpAddress());
            if (nextHop == null) {
                System.out.println("No route to destination from " + currentDevice);
                break;
            }
    
            System.out.println("Packet forwarded from " + currentDevice + " to " + nextHop);
    
            // Simulate packet loss with a probability
            double lossProbability = 0.05; // Adjust as needed
            if (Math.random() < lossProbability) {
                System.out.println("Packet lost during transmission.");
                totalPacketsLost++;
                return;
            }
    
            currentDevice = nextHop;
    
            try {
                Thread.sleep(100); // Simulate transmission delay
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    
        if (currentDevice.equals(destinationDevice)) {
            System.out.println("Packet successfully delivered to " + destinationDevice);
            totalPacketsForwarded++;
        }
    }

    public void stopSimulation() {
        isSimulating = false;
        System.out.println("Simulation stopped.");
        System.out.println("Total packets forwarded: " + totalPacketsForwarded);
        System.out.println("Total packets lost: " + totalPacketsLost);
    }

    public Map<String, NetworkDevice> getDevices() {
        return devices;
    }

    public List<Route> getRoutes() {
        return routes;
    }

    class NetworkDevice {
        private String name;
        private String ipAddress;
        private String subnetMask;
        private List<RouteEntry> routingTable; // Sorted by prefix length

        public NetworkDevice(String name, String ipAddress, String subnetMask) {
            this.name = name;
            this.ipAddress = ipAddress;
            this.subnetMask = subnetMask;
            this.routingTable = new ArrayList<>();
        }

        public String getName() {
            return name;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public String findNextHop(String destinationIp) {
            for (RouteEntry route : routingTable) {
                if (isInSubnet(destinationIp, route.getDestinationIp(), route.getPrefixLength())) {
                    return route.getNextHop();
                }
            }
            return null;
        }

        public void addRoute(RouteEntry route) {
            routingTable.add(route);
            routingTable.sort((r1, r2) -> Integer.compare(r2.getPrefixLength(), r1.getPrefixLength())); // Longest prefix first
        }
    }

    class RouteEntry {
        private String destinationIp;
        private int prefixLength;
        private String nextHop;

        public RouteEntry(String destinationIp, int prefixLength, String nextHop) {
            this.destinationIp = destinationIp;
            this.prefixLength = prefixLength;
            this.nextHop = nextHop;
        }

        public String getDestinationIp() {
            return destinationIp;
        }

        public int getPrefixLength() {
            return prefixLength;
        }

        public String getNextHop() {
            return nextHop;
        }
    }

    class Route {
        private String sourceDevice;
        private String destinationIpWithMask;

        public Route(String sourceDevice, String destinationIpWithMask) {
            this.sourceDevice = sourceDevice;
            this.destinationIpWithMask = destinationIpWithMask;
        }

        public String getSourceDevice() {
            return sourceDevice;
        }

        public String getDestinationIpWithMask() {
            return destinationIpWithMask;
        }
    }
}
