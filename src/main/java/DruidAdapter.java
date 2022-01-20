import java.util.*;
import java.util.concurrent.TimeUnit;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.*;

// TODO:
// 1. Customize table names
// *. port, user, password is not used

public class DruidAdapter implements BaseAdapter {

    // TODO modify table names
    private final String dataSourceName = "price_table"; // table storing market prices
    private final String baseTableName = "base_table"; // table storing financial instrument information
    private final String splitTableName = "split_table"; // table storing split events

    private String writeURL = ":8081/druid/indexer/v1/task";
    private String queryURL = ":8082/druid/v2/sql/avatica-protobuf/;serialization=protobuf";
    private static final Integer TIMEOUT = 500000; // default, used for client builder timeouts
    private final MediaType MEDIA_TYPE_TEXT = MediaType.parse("application/json");

    //// helper functions ////
    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient().newBuilder()
            .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
            .build();

    public static OkHttpClient getOkHttpClient() {
        return OK_HTTP_CLIENT;
    }

    // execute a already-built, http request from an okHttpClient
    // output the response code and return the time elapsed
    private double exeOkHttpRequest(Request request) {
        double costTime = 0L;
        Response response;
        OkHttpClient client = getOkHttpClient();
        try {
            double startTime = System.nanoTime();
            System.out.println("sending a request");
            response = client.newCall(request).execute();
            System.out.println("received a response:");
            int code = response.code();
            System.out.println("code " + code + ", " + response.body().string() + "\n");
            response.close();
            double endTime = System.nanoTime();
            costTime = endTime - startTime;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
        return costTime / 1000 / 1000;
    }

    public static void appendToFile(String filename, String msg) {
        File tmp = new File(filename);
        try {
            tmp.createNewFile();
            FileOutputStream oFile = new FileOutputStream(tmp, true);
            oFile.write(msg.getBytes());
            oFile.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return;
    }

    //// Adapter functions ////
    @Override
    public void initConnect(String ip, String port, String user, String password) {
        writeURL = "http://" + ip + writeURL;
        queryURL = "jdbc:avatica:remote:url=" + "http://" + ip + queryURL;
    }

    @Override
    public double insertData() {
        double costTime = 0L;
        String json = """
{
  "type": "index",
  "spec": {
    "ioConfig": {
      "type": "index",
      "inputSource": {
        "type": "local",
        "baseDir": "/home/ivanclkwong/price-3000-4000.csv",
        "filter": "*.csv"
      },
      "inputFormat": {
        "type": "csv",
        "findColumnsFromHeader": true,
        "skipHeaderRows": 0
      }
    },
    "tuningConfig": {
      "type": "index",
      "partitionsSpec": {
        "type": "dynamic"
      }
    },
    "dataSchema": {
      "dataSource": "price_table",
      "timestampSpec": {
        "column": "TradeDate",
        "format": "yyyy-MM-dd"
      },
      "dimensionsSpec": {
        "dimensions": [
          "Id",
          {
            "type": "double",
            "name": "HighPrice"
          },
          {
            "type": "double",
            "name": "LowPrice"
          },
          {
            "type": "double",
            "name": "ClosePrice"
          },
          {
            "type": "double",
            "name": "OpenPrice"
          },
          {
            "type": "long",
            "name": "Volume"
          }
        ]
      },
      "granularitySpec": {
        "queryGranularity": "none",
        "rollup": false,
        "segmentGranularity": "day"
      }
    }
  }
}
		""";

        // build a POST request
        Request request = null;
        try {
            request = new Request.Builder()
                    .url(writeURL)
                    .post(RequestBody.create(MEDIA_TYPE_TEXT, json.getBytes("UTF-8")))
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Unexpected error in sending request!");
        }

        // execute the request
        costTime = exeOkHttpRequest(request);
        return costTime;
    }

    public double query1() throws SQLException, ClassNotFoundException {
        Class.forName("org.apache.calcite.avatica.remote.Driver");
        Properties properties = new Properties();
        String query = String.format("""
				SELECT Id, EXTRACT(YEAR FROM __time), EXTRACT(MONTH FROM __time), 
				AVG("ClosePrice"), MAX("ClosePrice"), MIN("ClosePrice")
				FROM %s
				WHERE EXTRACT(YEAR FROM __time) >= 2022 AND EXTRACT(YEAR FROM __time) < 2032
				GROUP BY Id, EXTRACT(YEAR FROM __time), EXTRACT(MONTH FROM __time)
				ORDER BY Id, EXTRACT(YEAR FROM __time), EXTRACT(MONTH FROM __time)
				""", dataSourceName);

        double startTime, endTime;
        try (Connection connection = DriverManager.getConnection(queryURL, properties)) {
            try (final PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                startTime = System.nanoTime();
                final ResultSet resultSet = preparedStatement.executeQuery();
                endTime = System.nanoTime();
                resultSet.close();
            }
        }
	double accTime = endTime - startTime;
	String query2 = String.format("""
				SELECT Id, EXTRACT(YEAR FROM __time), 
				AVG("ClosePrice"), MAX("ClosePrice"), MIN("ClosePrice")
				FROM %s
				WHERE EXTRACT(YEAR FROM __time) >= 2022 AND EXTRACT(YEAR FROM __time) < 2032
				GROUP BY Id, EXTRACT(YEAR FROM __time)
				ORDER BY Id, EXTRACT(YEAR FROM __time)
				""", dataSourceName);
	try (Connection connection = DriverManager.getConnection(queryURL, properties)) {
            try (final PreparedStatement preparedStatement = connection.prepareStatement(query2)) {
                startTime = System.nanoTime();
                final ResultSet resultSet = preparedStatement.executeQuery();
                endTime = System.nanoTime();
                resultSet.close();
            }
        }
        return (accTime + endTime - startTime) / 1000 / 1000;
    }

    public double query2() throws SQLException, ClassNotFoundException {
        Class.forName("org.apache.calcite.avatica.remote.Driver");
        Properties properties = new Properties();
        String query = String.format("""
				SELECT a.Id, a.__time, LowPrice, HighPrice
				FROM %s a, %s b
				WHERE a.Id=b.Id AND a.__time=b.__time
				""", dataSourceName, splitTableName);

        double startTime, endTime;

        try (Connection connection = DriverManager.getConnection(queryURL, properties)) {
            try (final PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                startTime = System.nanoTime();
                final ResultSet resultSet = preparedStatement.executeQuery();
                endTime = System.nanoTime();
                resultSet.close();
            }
        }

        return (endTime - startTime) / 1000 / 1000;
    }

    public double query3() throws SQLException, ClassNotFoundException {
        Class.forName("org.apache.calcite.avatica.remote.Driver");
        Properties properties = new Properties();
        String query = String.format("""
				SELECT AVG("ClosePrice") FROM %s a, %s b
				WHERE a.Id = b.Id AND b.SIC='COMPUTERS'
				""", dataSourceName, baseTableName);

        double startTime, endTime;

        try (Connection connection = DriverManager.getConnection(queryURL, properties)) {
            try (final PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                startTime = System.nanoTime();
                final ResultSet resultSet = preparedStatement.executeQuery();
                endTime = System.nanoTime();
                resultSet.close();
            }
        }

        return (endTime - startTime) / 1000 / 1000;
    }

	/*
	public double query1a() throws SQLException, ClassNotFoundException {
		Class.forName("org.apache.calcite.avatica.remote.Driver");
		Properties properties = new Properties();
		String query = String.format("""
				SELECT Id, EXTRACT(YEAR FROM __time), AVG("ClosePrice"), MAX("ClosePrice"), MIN("ClosePrice")
				FROM %s
				WHERE EXTRACT(YEAR FROM __time) >= 2022 AND EXTRACT(YEAR FROM __time) < 2032
				GROUP BY Id, EXTRACT(YEAR FROM __time)
				ORDER BY Id, EXTRACT(YEAR FROM __time)
				""", dataSourceName);

		double startTime, endTime;
        try (Connection connection = DriverManager.getConnection(queryURL, properties)) {
        	try (final PreparedStatement preparedStatement = connection.prepareStatement(query)) {
        		startTime = System.nanoTime();
        		final ResultSet resultSet = preparedStatement.executeQuery();
        		endTime = System.nanoTime();
        		resultSet.close();
        	}
        }
		return (endTime - startTime) / 1000 / 1000;
	}

	public double query1b() throws SQLException, ClassNotFoundException {
		Class.forName("org.apache.calcite.avatica.remote.Driver");
		Properties properties = new Properties();
		String query = String.format("""
				SELECT Id, EXTRACT(YEAR FROM __time), EXTRACT(MONTH FROM __time), AVG("ClosePrice"), MAX("ClosePrice"), MIN("ClosePrice")
				FROM %s
				WHERE EXTRACT(YEAR FROM __time) >= 2022 AND EXTRACT(YEAR FROM __time) < 2032
				GROUP BY Id, EXTRACT(YEAR FROM __time), EXTRACT(MONTH FROM __time)
				ORDER BY Id, EXTRACT(YEAR FROM __time), EXTRACT(MONTH FROM __time)
				""", dataSourceName);

		double startTime, endTime;
        try (Connection connection = DriverManager.getConnection(queryURL, properties)) {
        	try (final PreparedStatement preparedStatement = connection.prepareStatement(query)) {
        		startTime = System.nanoTime();
        		final ResultSet resultSet = preparedStatement.executeQuery();
        		endTime = System.nanoTime();
        		resultSet.close();
        	}
        }

		return (endTime - startTime) / 1000 / 1000;
	}

	public double query1c() throws SQLException, ClassNotFoundException {
		Class.forName("org.apache.calcite.avatica.remote.Driver");
		Properties properties = new Properties();
		String query = String.format("""
				SELECT Id, EXTRACT(YEAR FROM __time), EXTRACT(DAY FROM __time), AVG("ClosePrice"), MAX("ClosePrice"), MIN("ClosePrice")
				FROM %s
				WHERE EXTRACT(YEAR FROM __time) >= 2022 AND EXTRACT(YEAR FROM __time) < 2032
				GROUP BY Id, EXTRACT(YEAR FROM __time), EXTRACT(DAY FROM __time)
				ORDER BY Id, EXTRACT(YEAR FROM __time), EXTRACT(DAY FROM __time)
				""", dataSourceName);

		double startTime, endTime;
        try (Connection connection = DriverManager.getConnection(queryURL, properties)) {
        	try (final PreparedStatement preparedStatement = connection.prepareStatement(query)) {
        		startTime = System.nanoTime();
        		final ResultSet resultSet = preparedStatement.executeQuery();
        		endTime = System.nanoTime();
        		resultSet.close();
        	}
        }

		return (endTime - startTime) / 1000 / 1000;
	}
	*/
}
