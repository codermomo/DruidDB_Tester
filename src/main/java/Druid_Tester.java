import java.sql.SQLException;

public class Druid_Tester {
    public static void main(String[] args) {
        String ip = "34.150.9.50", port = "", user = "", password = "";
        int numLoops = 1;

        BaseAdapter adapter = null;
        try {
            adapter = new DruidAdapter();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        adapter.initConnect(ip, port, user, password);

        // header in output file
        DruidAdapter.appendToFile("Druid DB Query.csv", "\"Trial\",\"Q1a\",\"Q1b\",\"Q1c\",\"Q2(Q3)\",\"Q3(Q4)\"\n");

        System.out.println("Run " + numLoops + " loops (Q1 -> Q2 -> Q3 -> Q1 -> ...):");

        Double avgQ1a = 0.0, avgQ1b = 0.0, avgQ1c = 0.0, avgQ2 = 0.0, avgQ3 = 0.0;

        for (int i = 0; i < numLoops; i++) {
            System.out.println("~trial " + i + "~");
            // print trial
            DruidAdapter.appendToFile("Druid DB Query.csv", "\"" + i + "\",");
            // print Q1
            // Double query1result = query1(adapter);
            Double query1aresult = query1a(adapter);
            avgQ1a += query1aresult;
            DruidAdapter.appendToFile("Druid DB Query.csv", "\"" + query1aresult + "\",");
            Double query1bresult = query1b(adapter);
            avgQ1b += query1bresult;
            DruidAdapter.appendToFile("Druid DB Query.csv", "\"" + query1bresult + "\",");
            Double query1cresult = 0.0;
            avgQ1c += query1cresult;
            DruidAdapter.appendToFile("Druid DB Query.csv", "\"" + query1cresult + "\",");
            // print Q2
            Double query2result = 0.0;
            avgQ2 += query2result;
            DruidAdapter.appendToFile("Druid DB Query.csv", "\"" + query2result + "\",");
            // print Q3
            Double query3result = 0.0;
            avgQ3 += query3result;
            DruidAdapter.appendToFile("Druid DB Query.csv", "\"" + query3result + "\"\n");
        }

        avgQ1a /= numLoops;
        avgQ2 /= numLoops;
        avgQ3 /= numLoops;
        DruidAdapter.appendToFile("Druid DB Query.csv", "\"avg\",\"" + avgQ1a +
                "\",\"" + avgQ1b + "\",\"" + avgQ1c + "\",\"" + avgQ2 + "\",\"" + avgQ3 + "\"\n");

        System.out.println("Finished all trials");


    }

    private static String importData(BaseAdapter adapter) {
        double time = adapter.insertData();
        return "Time spent for receiving respond (not ingestion) is " + time;
    }

    private static Double query1a(BaseAdapter adapter) {
        try {
            return adapter.query1a();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return 0.0;
    }

    private static Double query1b(BaseAdapter adapter) {
        try {
            return adapter.query1b();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return 0.0;
    }

    private static Double query1c(BaseAdapter adapter) {
        try {
            return adapter.query1c();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return 0.0;
    }

    private static Double query2(BaseAdapter adapter) {
        try {
            return adapter.query2();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return 0.0;
    }

    private static Double query3(BaseAdapter adapter) {
        try {
            return adapter.query3();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return 0.0;
    }
}
