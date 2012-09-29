import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;

public class Derby
{
    /* the default framework is embedded*/
    private String framework = "embedded";
    private String driver = "org.apache.derby.jdbc.EmbeddedDriver";
    private String protocol = "jdbc:derby:";
    
    Connection conn = null;
	/* This ArrayList usage may cause a warning when compiling this class
	 * with a compiler for J2SE 5.0 or newer. We are not using generics
	 * because we want the source to support J2SE 1.4.2 environments. */
    ArrayList statements = new ArrayList(); // list of Statements, PreparedStatements
    PreparedStatement entryInsert = null;
    PreparedStatement entryUpdate = null;
    PreparedStatement entryDelete = null;
    
    PreparedStatement eventInsert = null;
    PreparedStatement eventUpdate = null;
    PreparedStatement eventDelete = null;
    PreparedStatement eventQuery = null;
    
    PreparedStatement taskInsert = null;
    PreparedStatement taskUpdate = null;
    PreparedStatement taskDelete = null;
    PreparedStatement taskQuery = null;
	
	PreparedStatement dayTasksQuery = null;
	PreparedStatement dayEventsQuery = null;
	PreparedStatement pinboardQuery = null;
	
    
    Statement s = null;
    ResultSet rs = null;

    //ARGS
    //embedded - Use embedded database (DEFAULT)
    //derbyclient - Use client model to connect to a server

    public Derby() {

        System.out.println("Derby starting in " + framework + " mode");

        /* load the desired JDBC driver */
        loadDriver();
        
        try
        {
            setDBSystemDir();
            String dbName = "derbyDB"; // the name of the database
      
            conn = DriverManager.getConnection(protocol + dbName
                    + ";create=true");

            System.out.println("Connected to and created database " + dbName);

            // We want to control transactions manually. Autocommit is on by
            // default in JDBC.
            conn.setAutoCommit(false);

            /* Creating a statement object that we can use for running various
             * SQL statements commands against the database.*/
            s = conn.createStatement();
            statements.add(s);
            // We create a table...
            
            DatabaseMetaData dmd = conn.getMetaData();
            ResultSet tables = dmd.getTables(null, null, null, new String[]{"event","task"});
            
            createTables();       	
        	
            entryInsert = conn.prepareStatement("INSERT INTO entry (name, description, year, month, day, hour, minute) VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            statements.add(entryInsert);
            
            entryUpdate = conn.prepareStatement("UPDATE entry SET name=?, description=?, year=?, month=?, day=?, hour=?, minute=? WHERE E_id=?");
            statements.add(entryUpdate);
            
            entryDelete = conn.prepareStatement("DELETE FROM entry WHERE E_id=?");
            statements.add(entryDelete);

            eventInsert = conn.prepareStatement("INSERT INTO event (E_id, isAllDay, endYear, endMonth, endDay, endHour, endMinute, repeating) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            statements.add(eventInsert);
            
            eventUpdate = conn.prepareStatement("UPDATE event SET isAllDay=?, endYear=?, endMonth=?, endDay=?, endHour=?, endMinute=?, repeating=? WHERE E_id=?");
            statements.add(eventUpdate);       
            
            eventDelete = conn.prepareStatement("DELETE FROM event WHERE E_id=?");
            statements.add(eventDelete);
            
            eventQuery = conn.prepareStatement("SELECT entry.E_id entry.name, entry.description, entry.year, entry.month, entry.day, entry.hour, entry.minute, event.isAllDay, event.endYear, event.endMonth, event.endDay, event.endHour, event.endMinute, event.repeating FROM entry JOIN event ON entry.E_id=event.E_id");
            statements.add(eventQuery);
            
            taskInsert = conn.prepareStatement("INSERT INTO task (E_id, status, priority) VALUES (?, ?, ?)");
            statements.add(taskInsert);
            
            taskUpdate = conn.prepareStatement("UPDATE task SET status=?, priority=? where E_id=?");
            statements.add(taskUpdate);       
            
            taskDelete = conn.prepareStatement("DELETE from task WHERE E_id=?");
            statements.add(taskDelete);
            
            taskQuery = conn.prepareStatement("SELECT entry.E_id entry.name, entry.description, entry.year, entry.month, entry.day, entry.hour, entry.minute, task.status, task.priority FROM entry JOIN task ON entry.E_id=task.E_id");
			
			dayTasksQuery = conn.prepareStatement("SELECT entry.E_id  entry.name, entry.description, entry.year, entry.month, entry.day, entry.hour, entry.minute, task.status, task.priority FROM entry JOIN task ON entry.E_id=task.E_id WHERE *entry.year=? AND entry.month=? AND entry.day=?)");
			statements.add(dayTasksQuery);
			
			dayEventsQuery = conn.prepareStatement("SELECT entry.E_id entry.name, entry.description, entry.year, entry.month, entry.day, entry.hour, entry.minute, event.isAllDay, event.endYear, event.endMonth, event.endDay, event.endHour, event.endMinute, event.repeating FROM entry JOIN event ON entry.E_id=event.E_id WHERE (entry.year=? AND entry.month=? AND entry.day=?)");
			statements.add(dayEventsQuery);
			
			pinboardQuery = conn.prepareStatement("SELECT entry.E_id entry.name, entry.description, entry.year, entry.month, entry.day, entry.hour, entry.minute, task.status, task.priority FROM entry JOIN task ON entry.E_id=task.E_id WHERE (entry.year>?) OR (entry.year=? AND entry.month>?) OR (entry.year=? AND entry.month=? AND entry.day=>?) AND task.status=?");
			statements.add(pinboardQuery);
            /* 
             * Normally, it is best to use a pattern of
             *  while(rs.next()) {
             *    // do something with the result set
             *  }
             * to process all returned rows, but we are only expecting two rows
             * this time, and want the verification code to be easy to
             * comprehend, so we use a different pattern.
             */
            
        }
        catch (SQLException sqle)
        {
            printSQLException(sqle);
        } finally {
            // release all open resources to avoid unnecessary memory usage

            handle();
            
        }
    }
    
    private void setDBSystemDir() {
        System.setProperty("derby.system.home", System.getProperty("user.home")
            + "asdf-calendar/DerbyPrototype");
    }

    private void createTables() { //If the tables already exist, this code block will NOT execute
    	System.out.println("Creating Tables");
    	try {
    		s.execute("CREATE TABLE entry(E_id int NOT NULL AUTO_INCREMENT PRIMARY KEY, name varchar(255), description varchar(255), year int, month int, day int, hour int, minute int");
            System.out.println("Created table entry");
    	} catch (SQLException e) {}
    	try {
    		s.execute("CREATE TABLE event(E_id int NOT NULL PRIMARY KEY, isAllDay boolean, endYear int, endMonth int, endDay int, endHour int, endMinute int, repeating smallint)");
            System.out.println("Created table event");
    	} catch(SQLException e) {}
    	try {
    		s.execute("CREATE TABLE task(E_id int NOT NULL PRIMARY KEY, status smallint, priority smallint)");
            System.out.println("Created table task");
    	} catch (SQLException e) {}
    }
    
    private void handle() {
    	try {
            if (rs != null) {
                rs.close();
                rs = null;
            }
        } catch (SQLException sqle) {
            printSQLException(sqle);
        }

        // Statements and PreparedStatements
        int i = 0;
        while (!statements.isEmpty()) {
            // PreparedStatement extend Statement
            Statement st = (Statement)statements.remove(i);
            try {
                if (st != null) {
                    st.close();
                    st = null;
                }
            } catch (SQLException sqle) {
                printSQLException(sqle);
            }
        }

        //Connection
        try {
            if (conn != null) {
                conn.close();
                conn = null;
            }
        } catch (SQLException sqle) {
            printSQLException(sqle);
        }
    }
    
    public void addEvent(String name, String desc, int year, int month, int day, int hour, int minute, boolean isAllDay, int endYear, int endMonth, int endDay, int endHour, int endMinute, int repeating) {
    	try {
    		entryInsert.setString(1, name);
    		entryInsert.setString(2, desc);
    		entryInsert.setInt(3, year);
			entryInsert.setInt(4, month);
			entryInsert.setInt(5, day);
			entryInsert.setInt(6, hour);
			entryInsert.setInt(7, minute);
    		entryInsert.executeUpdate();
    		
    		ResultSet id = entryInsert.getGeneratedKeys();
    		
    		int E_id = id.getInt(1); //Grab the Primary Key of the Entry to be used as a Foreign Key for Event
    		
    		eventInsert.setInt(1, E_id);
            eventInsert.setBoolean(2, isAllDay);
            entryInsert.setInt(3, endYear);
			entryInsert.setInt(4, endMonth);
			entryInsert.setInt(5, endDay);
			entryInsert.setInt(6, endHour);
			entryInsert.setInt(7, endMinute);
            eventInsert.setInt(8, repeating);
            eventInsert.executeUpdate();
    	} catch (SQLException e) {
    		printSQLException(e);
    	} finally {
    		handle();
    	}
    }
    
    public void updateEvent(int id, String name, String desc, int year, int month, int day, int hour, int minute, boolean isAllDay, int endDay, int endYear, int endHour, int endMonth, int endMinute, int repeating) {
    	try {
    		entryUpdate.setString(1, name);
            entryUpdate.setString(2, desc);
            entryUpdate.setInt(3, year);
			entryUpdate.setInt(4, month);
			entryUpdate.setInt(5, day);
			entryUpdate.setInt(6, hour);
			entryUpdate.setInt(7, minute);
			entryUpdate.setInt(9, id);
            entryUpdate.executeUpdate();
			
			eventUpdate.setBoolean(1, isAllDay);
			eventUpdate.setInt(2, endYear);
			eventUpdate.setInt(3, endMonth);
			eventUpdate.setInt(4, endDay);
			eventUpdate.setInt(5, endHour);
			eventUpdate.setInt(6, endMinute);
			eventUpdate.setInt(7, repeating);
			eventUpdate.setInt(8, id);
			eventUpdate.executeUpdate();
			
			} catch(SQLException e) {
    		printSQLException(e);
    	} finally {
    		handle();
    	}
    }
    
    public void deleteEvent(int E_id) {
    	try {
    		entryDelete.setInt(1, E_id);
    		entryDelete.executeUpdate();
    		
    		eventDelete.setInt(1,E_id);
    		eventDelete.executeUpdate();
    		
    	} catch(SQLException e) {
    		printSQLException(e);
    	} finally {
    		handle();
    	}
    }
    
	/*
    public ResultSet queryEvent(String dateIn) {
    	try {
    		eventQuery.setString(1, dateIn);
    		
    		ResultSet rs = eventQuery.executeQuery();
    		
			return rs;
			
    	} catch(SQLException e) {
    		printSQLException(e);
    	} finally {
    		handle();
    	}
    }*/
	public ResultSet dayEventsQuery(int year, int month, int day){
		try {
    		dayEventsQuery.setInt(1, year);
			dayEventsQuery.setInt(2, month);
			dayEventsQuery.setInt(3, day);
    		
    		ResultSet rs = dayEventsQuery.executeQuery();
    		
			return rs;
    		
    	} catch(SQLException e) {
    		printSQLException(e);
    	} finally {
    		handle();
    	}
		return null;
	}
    
    public void addTask(String name, String desc, int year, int month, int day, int hour, int minute, int status, int priority) {
    	try {
    		entryInsert.setString(1, name);
    		entryInsert.setString(2, desc);
    		entryInsert.setInt(3, year);
			entryInsert.setInt(4, month);
			entryInsert.setInt(5, day);
			entryInsert.setInt(6, hour);
			entryInsert.setInt(7, minute);
    		entryInsert.executeUpdate();
    		
    		ResultSet id = entryInsert.getGeneratedKeys();
    		
    		int E_id = id.getInt(1); //Grab the Primary Key of the Entry to be used as a Foreign Key for Event
    		
    		taskInsert.setInt(1, E_id);
            taskInsert.setInt(2, status);
            taskInsert.setInt(3, priority);
            taskInsert.executeUpdate();
    	} catch(SQLException e) {
    		printSQLException(e);
    	} finally {
    		handle();
    	}
    }
    
    public void updateTask(int id, String name, String desc, int year, int month, int day, int hour, int minute, int status, int priority, String toReplace) {
    	try {
    		entryUpdate.setString(1, name);
            entryUpdate.setString(2, desc);
            entryUpdate.setInt(3, year);
			entryUpdate.setInt(4, month);
			entryUpdate.setInt(5, day);
			entryUpdate.setInt(6, hour);
			entryUpdate.setInt(7, minute);
			entryUpdate.setInt(9, id);
            entryUpdate.executeUpdate();
			
			taskUpdate.setInt(1, status);
			taskUpdate.setInt(2, priority);
			taskUpdate.setInt(3, id);
			taskUpdate.executeUpdate();
    	} catch(SQLException e) {
    		printSQLException(e);
    	} finally {
    		handle();
    	}
    }

    public void deleteTask(int E_id) {
    	try {
    		entryDelete.setInt(1, E_id);
    		entryDelete.executeUpdate();
    		
    		taskDelete.setInt(1,E_id);
    		taskDelete.executeUpdate();
    		
    	} catch(SQLException e) {
    		printSQLException(e);
    	} finally {
    		handle();
    	}
    }
    
	/*
    public ResultSet queryTask(String dateIn) {
    	try {
    		taskQuery.setString(1, dateIn);
    		
    		ResultSet rs = taskQuery.executeQuery();
    		
			return rs;
    		
    	} catch(SQLException e) {
    		printSQLException(e);
    	} finally {
    		handle();
    	}
    }*/
	
	public ResultSet dayTasksQuery(int year, int month, int day)
	{
		try {
    		dayTasksQuery.setInt(1, year);
			dayTasksQuery.setInt(2, month);
			dayTasksQuery.setInt(3, day);
    		
    		ResultSet rs = dayTasksQuery.executeQuery();
    		
			return rs;
    		
    	} catch(SQLException e) {
    		printSQLException(e);
    	} finally {
    		handle();
    	}
		
		return null;
	}
	
	public ResultSet pinboardQuery(int year, int month, int day, int status)
	{
		try
		{
			pinboardQuery.setInt(1, year);
			pinboardQuery.setInt(2, year);
			pinboardQuery.setInt(4, year);
			
			pinboardQuery.setInt(3, month);
			pinboardQuery.setInt(5, month);
			
			pinboardQuery.setInt(6, day);
			
			pinboardQuery.setInt(7, status);
			
			ResultSet rs = pinboardQuery.executeQuery();
    		
			return rs;
			
		} catch(SQLException e) {
    		printSQLException(e);
    	} finally {
    		handle();
    	}
		
		return null;
	}
    
    public void updateDatabase() {
    	try {
    		conn.commit();
    	} catch (SQLException e) {
    		printSQLException(e);
    	} finally {
    		handle();
    	}
    }
    
    public void closeDatabase() {
		/*
         * In embedded mode, an application should shut down the database.
         * If the application fails to shut down the database,
         * Derby will not perform a checkpoint when the JVM shuts down.
         * This means that it will take longer to boot (connect to) the
         * database the next time, because Derby needs to perform a recovery
         * operation.
         *
         * It is also possible to shut down the Derby system/engine, which
         * automatically shuts down all booted databases.
         *
         * Explicitly shutting down the database or the Derby engine with
         * the connection URL is preferred. This style of shutdown will
         * always throw an SQLException.
         *
         * Not shutting down when in a client environment, see method
         * Javadoc.
         */
    	
        if (framework.equals("embedded"))
        {
            try
            {
                // the shutdown=true attribute shuts down Derby
                DriverManager.getConnection("jdbc:derby:;shutdown=true");

                // To shut down a specific database only, but keep the
                // engine running (for example for connecting to other
                // databases), specify a database in the connection URL:
                //DriverManager.getConnection("jdbc:derby:" + dbName + ";shutdown=true");
            }
            catch (SQLException se)
            {
                if (( (se.getErrorCode() == 50000)
                        && ("XJ015".equals(se.getSQLState()) ))) {
                    // we got the expected exception
                    System.out.println("Derby shut down normally");
                    // Note that for single database shutdown, the expected
                    // SQL state is "08006", and the error code is 45000.
                } else {
                    // if the error code or SQLState is different, we have
                    // an unexpected exception (shutdown failed)
                    System.err.println("Derby did not shut down normally");
                    printSQLException(se);
                }
            }
        }
    }
    
    //Loads the embedded driver
    private void loadDriver() {
        /*
         *  The JDBC driver is loaded by loading its class.
         *  If you are using JDBC 4.0 (Java SE 6) or newer, JDBC drivers may
         *  be automatically loaded, making this code optional.
         *
         *  In an embedded environment, this will also start up the Derby
         *  engine (though not any databases), since it is not already
         *  running.
         */
        try {
            Class.forName(driver).newInstance();
            System.out.println("Loaded the appropriate driver");
        } catch (ClassNotFoundException cnfe) {
            System.err.println("\nUnable to load the JDBC driver " + driver);
            System.err.println("Please check your CLASSPATH.");
            cnfe.printStackTrace(System.err);
        } catch (InstantiationException ie) {
            System.err.println(
                        "\nUnable to instantiate the JDBC driver " + driver);
            ie.printStackTrace(System.err);
        } catch (IllegalAccessException iae) {
            System.err.println(
                        "\nNot allowed to access the JDBC driver " + driver);
            iae.printStackTrace(System.err);
        }
    }

    /**
     * Reports a data verification failure to System.err with the given message.
     *
     * @param message A message describing what failed.
     */
    private void reportFailure(String message) {
        System.err.println("\nData verification failed:");
        System.err.println('\t' + message);
    }

    /**
     * Prints details of an SQLException chain to <code>System.err</code>.
     * Details included are SQL State, Error code, Exception message.
     *
     * @param e the SQLException from which to print details.
     */
    public static void printSQLException(SQLException e)
    {
        // Unwraps the entire exception chain to unveil the real cause of the
        // Exception.
        while (e != null)
        {
            System.err.println("\n----- SQLException -----");
            System.err.println("  SQL State:  " + e.getSQLState());
            System.err.println("  Error Code: " + e.getErrorCode());
            System.err.println("  Message:    " + e.getMessage());
            // for stack traces, refer to derby.log or uncomment this:
            //e.printStackTrace(System.err);
            e = e.getNextException();
        }
    }    
}