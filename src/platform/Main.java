/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package platform;

import platform.server.data.sql.DataAdapter;
import platform.server.data.sql.PostgreDataAdapter;
import platform.server.logics.classes.RemoteClass;
import platform.server.logics.BusinessLogics;
import platform.server.logics.session.DataSession;
import platform.server.view.navigator.RemoteNavigator;
import platform.client.layout.Layout;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;

import custom.TmcBusinessLogics;
import test.TestBusinessLogics;

public class Main {

    RemoteClass[] ClassList;

    static RemoteNavigator Navigator;

    public static Layout Layout;

    static DataAdapter Adapter;
    static DataSession Session = null;

    static boolean recreateDB = false;
    public static Integer forceSeed = -1; //1199; //3581
    public static int iterations = 7;
    public static boolean DebugFlag = false;
    public static boolean activateCaches = true;
    static boolean AllowNulls = false;
    
    public static boolean debugWatch = false;

    public static DataAdapter getDefault() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        return new PostgreDataAdapter("testplat","localhost");
//        return new MySQLDataAdapter("testplat","localhost");
//          return new MSSQLDataAdapter();
    }

    public static void main(String[] args) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {

/*        int size = 1;
        List<Integer> perm = new ArrayList<Integer>();
        for(int i=0;i<size;i++)
            perm.add(i);
        for(Map<Integer,Integer> map : new Pairs<Integer,Integer>(perm,perm)) {
            for(int i=0;i<size;i++)
                System.out.print(map.get(i)+" ");
            System.out.println();
        }
        if(1==1) return;*/
  
  
/*        Where a = new TestDataWhere("a");
        Where b = new TestDataWhere("b");
        Where c = new TestDataWhere("c");
        Where d = new TestDataWhere("d");
        Where x = new TestDataWhere("x");
        Where y = new TestDataWhere("y");
        System.out.println(a.and(b).and(a.not())); // a.b.a' = FALSE
        System.out.println(a.not().and(c.not()).or(d.and(a.or(c)))); // a'.c'+d.(a+c) = a'.c'+d
        System.out.println(a.or(b).and(a.or(b.not()))); // (a+b).(a+b') = a
        System.out.println(a.not().or(d.and(a))); // a'+d.a = a'+d
        System.out.println(a.not().or(d.or(a).and(b))); // a'+(b.(d+a)) = a'+b
        System.out.println(a.or(b.not().and(b.not()))); // a+b'.b' = a+b'
        System.out.println(a.and(b.not()).or(a.not().and(b)).and(a.not().and(b))); // (a.b'+a'b).(a'.b)
        System.out.println(a.and(b).or(a.not().and(b.not())).and(a.and(x).or(a.not().and(y)))); // (a.b+a'.b').(a.x+a'.y)
        System.out.println(a.and(b).followFalse(a.not().or(b.not()))); // a.b.a' = FALSE

        Where result = new AndWhere();
        Where wb = new TestDataWhere("b");
        for(int i=0;i<6;i++) {
            Where iteration = new OrWhere();
            iteration = iteration.or(wb);
            for(int j=0;j<4;j++)
                iteration = iteration.or(new TestDataWhere("w"+i+"_"+j));
            result = result.and(iteration);
        }
        System.out.println(result);

        WhereTester test = new WhereTester();
        test.test();
        if(1==1) return;*/
/*        new SourceTest();
        if(1==1) return;*/

        if(forceSeed ==null || forceSeed !=-1) {
            while(true) {
                System.out.println("Opened");
                new TmcBusinessLogics(1);
//            System.out.println("Closed");
//            try {
//                new TestBusinessLogics(0);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            System.out.println("Suspicious");
//            new TmcBusinessLogics(-1);
            }
        }
/*        UnionQuery<KeyField,PropertyField> Union = new UnionQuery<KeyField,PropertyField>(Table1.Keys,1);

        JoinQuery<KeyField,PropertyField> Query = Union.newJoinQuery(1);

        Join<KeyField,PropertyField> TableJoin = new UniJoin<KeyField,PropertyField>(Table1,Query);
        Query.Wheres.add(new Where(TableJoin));

        Join<KeyField,PropertyField> Table2Join = new Join<KeyField,PropertyField>(Table2);
        Table2Join.Joins.put(Table2Key1,new JoinExpr<KeyField,PropertyField>(TableJoin,Table1Prop1,true));
//        Table2Join.Joins.put(Table2Key1,Query.MapKeys.get(Table1Key1));
        Query.properties.put(Table1Prop1,new JoinExpr<KeyField,PropertyField>(Table2Join,Table2Prop1,true));

        Join<KeyField,PropertyField> Table2Join2 = new Join<KeyField,PropertyField>(Table2);
        Table2Join2.Joins.put(Table2Key1,new JoinExpr<KeyField,PropertyField>(TableJoin,Table1Prop1,true));
        Query.properties.put(Table1Prop2,new JoinExpr<KeyField,PropertyField>(Table2Join2,Table2Prop2,true));

        FormulaSourceExpr Formula = new FormulaSourceExpr("prm1=3");
        Formula.Params.put("prm1",new JoinExpr<KeyField,PropertyField>(Table2Join2,Table2Prop2,true));
        Query.properties.put(Table1Prop3,new FormulaWhereSourceExpr(Formula,true));

        JoinQuery<KeyField,PropertyField> Query2 = Union.newJoinQuery(1);
        Join<KeyField,PropertyField> Q2TableJoin = new UniJoin<KeyField,PropertyField>(Table1,Query2);
        Query2.Wheres.add(new Where(Q2TableJoin));

        Query2.properties.put(Table1Prop1,new JoinExpr<KeyField,PropertyField>(Q2TableJoin,Table1Prop1,false));
  */
 //       List<String> GroupKeys = new ArrayList();
//        GroupKeys.add("value");
//        GroupQuery<String,String,String> GroupQuery = new GroupQuery<String,String,String>(GroupKeys,Union,"value2",0);

        // сначала закинем KeyField'ы и прогоним Select
/*        Map<KeyField,String> KeyNames = new HashMap();
        Map<String,String> PropertyNames = new HashMap();
        Query.fillSelectNames(KeyNames,PropertyNames);
        System.out.println(Union.getSelect(KeyNames,PropertyNames));*/

//        System.out.println((new ModifyQuery(Table1,Query)).getInsertLeftKeys());

/*        Map<String,String> KeyNames = new HashMap();
        Map<String,String> PropertyNames = new HashMap();
        GroupQuery.fillSelectNames(KeyNames,PropertyNames);
        System.out.println(GroupQuery.getSelect(KeyNames,PropertyNames));
*/
//        if(1==1) return;

        try {
            UIManager.setLookAndFeel(UIManager.getInstalledLookAndFeels()[2].getClassName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InstantiationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        try {

            Adapter = Main.getDefault();

            BusinessLogics BL = new TmcBusinessLogics();

            if (recreateDB) {

                Adapter.createDB();

                DataSession Session = BL.createSession(Adapter);
                BL.fillDB(Session, true);
                Session.close();

                BL.fillData(Adapter);
            } else
                BL.fillDB(null, false);

            // базовый навигатор
            RemoteNavigator<TestBusinessLogics> remoteNavigator =  new RemoteNavigator(Adapter,BL);

            remoteNavigator.changeCurrentUser("user1", "user1");

//            LoginDialog loginDialog = new LoginDialog(remoteNavigator);
//            if (loginDialog.login()) {
                Layout = new Layout(remoteNavigator);
                Layout.setVisible(true);
//            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
//        } catch (JRException ex) {
//            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

//          java.lang.Class.forName("net.sourceforge.jtds.jdbc.Driver");
//        java.lang.Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
//          Connection cn = DriverManager.getConnection("jdbc:jtds:sqlserver://mycomp:1433;namedPipe=true;User=sa;Password=");
//        Connection cn = DriverManager.getConnection("jdbc:sqlserver://server:1433;User=sa;Password=");

//        BusinessLogics t = new BusinessLogics();
//        t.FullDBTest();

//        Test t = new Test();
//        t.SimpleTest(null);
    }
}

