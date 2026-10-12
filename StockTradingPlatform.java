import java.io.*;
import java.util.*;

public class StockTradingPlatform {

    static class Stock {
        String symbol;
        double price;

        Stock(String symbol, double price) {
            this.symbol = symbol;
            this.price = price;
        }
    }

    static class Transaction {
        String type; // BUY or SELL
        String symbol;
        int qty;
        double price;

        Transaction(String type, String symbol, int qty, double price) {
            this.type = type;
            this.symbol = symbol;
            this.qty = qty;
            this.price = price;
        }
    }

    static class User {
        double cash = 10000;
        Map<String, Integer> holdings = new LinkedHashMap<>();
        List<Transaction> transactions = new ArrayList<>();
        List<Double> valueHistory = new ArrayList<>();
    }

    static final String FILE = "portfolio.txt";
    static Map<String, Stock> market = new LinkedHashMap<>();
    static User user = new User();
    static Scanner sc = new Scanner(System.in);
    static Random rnd = new Random();

    public static void main(String[] args) {
        initMarket();
        load();
        if (user.valueHistory.isEmpty()) user.valueHistory.add(portfolioValue());

        while (true) {
            System.out.println("\n=== Stock Trading Platform ===");
            System.out.println("1) Market data");
            System.out.println("2) Buy");
            System.out.println("3) Sell");
            System.out.println("4) My portfolio");
            System.out.println("5) Next day (update prices)");
            System.out.println("6) Transaction history");
            System.out.println("7) Save & exit");
            System.out.print("Choose: ");
            String c = sc.nextLine().trim();

            switch (c) {
                case "1": showMarket(); break;
                case "2": trade(true); break;
                case "3": trade(false); break;
                case "4": showPortfolio(); break;
                case "5": nextDay(); break;
                case "6": showTransactions(); break;
                case "7": save(); System.out.println("Saved. Bye!"); return;
                default: System.out.println("Invalid choice.");
            }
        }
    }

    static void initMarket() {
        market.put("AAPL", new Stock("AAPL", 180.0));
        market.put("GOOG", new Stock("GOOG", 140.0));
        market.put("TSLA", new Stock("TSLA", 250.0));
        market.put("AMZN", new Stock("AMZN", 130.0));
        market.put("MSFT", new Stock("MSFT", 330.0));
    }

    static void showMarket() {
        System.out.println("\n--- Market ---");
        System.out.printf("%-8s %10s%n", "Symbol", "Price");
        for (Stock s : market.values()) {
            System.out.printf("%-8s %10.2f%n", s.symbol, s.price);
        }
    }

    static void trade(boolean buy) {
        showMarket();
        System.out.print("Symbol: ");
        String sym = sc.nextLine().trim().toUpperCase();
        Stock stock = market.get(sym);
        if (stock == null) {
            System.out.println("Unknown symbol.");
            return;
        }

        System.out.print("Quantity: ");
        int qty;
        try {
            qty = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid quantity.");
            return;
        }
        if (qty <= 0) {
            System.out.println("Quantity must be positive.");
            return;
        }

        double cost = qty * stock.price;
        int owned = user.holdings.getOrDefault(sym, 0);

        if (buy) {
            if (cost > user.cash) {
                System.out.printf("Not enough cash. Need %.2f, have %.2f%n", cost, user.cash);
                return;
            }
            user.cash -= cost;
            user.holdings.put(sym, owned + qty);
            user.transactions.add(new Transaction("BUY", sym, qty, stock.price));
            System.out.printf("Bought %d %s for %.2f%n", qty, sym, cost);
        } else {
            if (qty > owned) {
                System.out.println("You only own " + owned + " shares.");
                return;
            }
            user.cash += cost;
            if (owned == qty) user.holdings.remove(sym);
            else user.holdings.put(sym, owned - qty);
            user.transactions.add(new Transaction("SELL", sym, qty, stock.price));
            System.out.printf("Sold %d %s for %.2f%n", qty, sym, cost);
        }
    }

    static double portfolioValue() {
        double total = user.cash;
        for (Map.Entry<String, Integer> e : user.holdings.entrySet()) {
            total += e.getValue() * market.get(e.getKey()).price;
        }
        return total;
    }

    static void showPortfolio() {
        System.out.println("\n--- Portfolio ---");
        System.out.printf("Cash: %.2f%n", user.cash);
        if (user.holdings.isEmpty()) {
            System.out.println("No stocks owned.");
        } else {
            System.out.printf("%-8s %6s %10s %12s%n", "Symbol", "Qty", "Price", "Value");
            for (Map.Entry<String, Integer> e : user.holdings.entrySet()) {
                double p = market.get(e.getKey()).price;
                System.out.printf("%-8s %6d %10.2f %12.2f%n", e.getKey(), e.getValue(), p, e.getValue() * p);
            }
        }

        double total = portfolioValue();
        double start = user.valueHistory.get(0);
        System.out.printf("Total value: %.2f%n", total);
        System.out.printf("Performance since start: %+.2f (%+.2f%%)%n", total - start, (total - start) / start * 100);

        System.out.print("Value history: ");
        for (double v : user.valueHistory) System.out.printf("%.0f ", v);
        System.out.println();
    }

    static void nextDay() {
        for (Stock s : market.values()) {
            double change = (rnd.nextDouble() - 0.5) * 0.10; // -5% to +5%
            s.price = Math.max(1, s.price * (1 + change));
        }
        user.valueHistory.add(portfolioValue());
        System.out.println("Prices updated.");
        showMarket();
    }

    static void showTransactions() {
        if (user.transactions.isEmpty()) {
            System.out.println("No transactions yet.");
            return;
        }
        System.out.println("\n--- Transactions ---");
        for (Transaction t : user.transactions) {
            System.out.printf("%-5s %-6s x%-4d @ %.2f%n", t.type, t.symbol, t.qty, t.price);
        }
    }

    // ---------- File I/O ----------
    static void save() {
        try (PrintWriter out = new PrintWriter(new FileWriter(FILE))) {
            out.println("CASH," + user.cash);
            for (Stock s : market.values()) out.println("PRICE," + s.symbol + "," + s.price);
            for (Map.Entry<String, Integer> e : user.holdings.entrySet())
                out.println("HOLD," + e.getKey() + "," + e.getValue());
            for (Transaction t : user.transactions)
                out.println("TX," + t.type + "," + t.symbol + "," + t.qty + "," + t.price);
            for (double v : user.valueHistory) out.println("HIST," + v);
        } catch (IOException e) {
            System.out.println("Could not save: " + e.getMessage());
        }
    }

    static void load() {
        File f = new File(FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                switch (p[0]) {
                    case "CASH": user.cash = Double.parseDouble(p[1]); break;
                    case "PRICE": market.get(p[1]).price = Double.parseDouble(p[2]); break;
                    case "HOLD": user.holdings.put(p[1], Integer.parseInt(p[2])); break;
                    case "TX":
                        user.transactions.add(new Transaction(p[1], p[2], Integer.parseInt(p[3]), Double.parseDouble(p[4])));
                        break;
                    case "HIST": user.valueHistory.add(Double.parseDouble(p[1])); break;
                }
            }
            System.out.println("Loaded saved portfolio.");
        } catch (Exception e) {
            System.out.println("Could not load saved data, starting fresh.");
            user = new User();
        }
    }
}
