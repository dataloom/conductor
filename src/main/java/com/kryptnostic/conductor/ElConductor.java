package com.kryptnostic.conductor;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ElConductor {
    private static final Logger logger = LoggerFactory.getLogger( ElConductor.class );

    public static void main( String[] args ) {
        SparkConf conf = new SparkConf().setAppName( "Kryptnostic Spark Datastore" )
//                .setMaster( "spark://mjolnir.local:7077" )
                .setMaster("local")
                .setJars( new String[] { "./kindling/build/libs/kindling-0.0.0-SNAPSHOT-all.jar" })
                .set("spark.cassandra.connection.host", "localhost");
//                .setJars( new String[] {
//                        "local:///Users/mtamayo/repos/kryptnostic/kryptnostic-conductor-client/build/libs/kryptnostic-conductor-client-0.0.0-SNAPSHOT.jar",
//                        "local:///Users/mtamayo/repos/kryptnostic/kryptnostic-conductor/build/libs/kryptnostic-conductor-0.0.0-SNAPSHOT-all.jar" } );
        JavaSparkContext spark = new JavaSparkContext( conf );
        JavaRDD<String> s = spark.textFile( "kryptnostic-conductor/src/main/resources/employees.csv" );
//        s.foreach( l -> System.out.println( l ) );
//        JavaRDD<Employee> t = s.map( e -> Employee.EmployeeCsvReader.getEmployee( e ) );
//        SQLContext context = new SQLContext( spark );
//        logger.info( "Total # of employees: {}", t.count() );
//        DataFrame df = context.createDataFrame( t, Employee.class );
//        df.registerTempTable( "employees" );
//        DataFrame emps = context.sql( "SELECT * from employees WHERE salary > 81500" );
//        List<String> highlyPaidEmps = emps.javaRDD().map( e -> String.format( "%s,%s,%s,%d",
//                e.getAs( "name" ),
//                e.getAs( "dept" ),
//                e.getAs( "title" ),
//                e.getAs( "salary" ) ) ).collect();
//        highlyPaidEmps.forEach( e -> logger.info( e ) );
//
//        logger.info( "emps: {}", Lists.newArrayList( emps.javaRDD().map( e -> new Employee(
//                e.getAs( "name" ),
//                e.getAs( "dept" ),
//                e.getAs( "title" ),
//                (int) e.getAs( "salary" ) ) ).collect() ) );


//        ConductorSparkImpl impl = new ConductorSparkImpl("keyspace", spark, new CassandraSQLContext(spark.sc()), CassandraJavaUtil.javaFunctions(spark), new SparkAuthorizationManager());
//        QueryResult result = impl.cacheQueryResult("test", s, Employee.class);
//        System.out.println(result.toString());

    }
}
