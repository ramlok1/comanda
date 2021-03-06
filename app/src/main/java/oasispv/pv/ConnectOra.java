package oasispv.pv;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;


public class ConnectOra {

    private static String driver, url;
    private static Connection conexion;
    private static ConnectOra dbInstance;
    private static boolean test;


    private ConnectOra() {

    }


    public static ConnectOra getInstance() {
        if (dbInstance == null) {
            dbInstance = new ConnectOra();
        }
        return dbInstance;
    }


    public static Connection getConexion() {

        if (conexion != null) {
            try {
                Statement s = conexion.createStatement();
                String tsql = "select 1 from dual";
                s.setQueryTimeout(3);
                ResultSet rs = s.executeQuery(tsql);
                if (rs.next()) {
                    test = true;
                }
            } catch (SQLException e) {
                test = false;
            }
        }
        if (conexion == null || test == false) {
            driver = "oracle.jdbc.driver.OracleDriver";


            url = "jdbc:oracle:thin:@" + variables.ip + ":1521:" + variables.cn;

            try {
                Class.forName(driver).newInstance();

                conexion = DriverManager.getConnection(url, variables.un, variables.pw);
                // revisan el log para ver que tira....

                System.out.println("Conexion a Base de Datos " + variables.cn + " Exitosa");

            } catch (Exception exc) {
                variables.erroroc = 1;
                System.out.println("Error al tratar de abrir la base de Datos "
                        + variables.cn + " : " + exc);
            }
        }
        return conexion;
    }

    public static Connection CerrarConexion() throws SQLException {
        conexion.close();
        conexion = null;
        return conexion;
    }

    public int getlogin(String usr, String pwd) throws SQLException {
        conexion = ConnectOra.getInstance().getConexion();
        int res;
        CallableStatement login = conexion.prepareCall("{? = call  cielo.check_login(?,?)}");
        login.registerOutParameter(1, java.sql.Types.INTEGER);
        login.setString(2, usr.toUpperCase());
        login.setString(3, pwd.toUpperCase());

        login.execute();
        //conexion.close();
        if (login.getInt(1) == 1) {
            res = 1;
        } else {
            res = 0;
        }
        return res;
    }

    public String genera_transa() throws SQLException {
        conexion = ConnectOra.getInstance().getConexion();
        String res;
        CallableStatement transa = conexion.prepareCall("{? = call  CIELOPV.GENERA_TRANSA_ENC}");
        transa.registerOutParameter(1, java.sql.Types.VARCHAR);


        transa.execute();
        //conexion.close();
        res = transa.getString(1);

        return res;
    }

    public String genera_transa_det() throws SQLException {
        conexion = ConnectOra.getInstance().getConexion();
        String res;
        CallableStatement transa = conexion.prepareCall("{? = call  CIELOPV.GENERA_TRANSA_DET}");
        transa.registerOutParameter(1, java.sql.Types.VARCHAR);


        transa.execute();
        //conexion.close();
        res = transa.getString(1);

        return res;
    }

    public void inserta_comanda_enc() throws SQLException {

        conexion = ConnectOra.getInstance().getConexion();
        String qry_fecha = "SELECT PR_FECHA FROM PVFRONT.FRPARAM";

        Statement s = conexion.createStatement();
        ResultSet r = s.executeQuery(qry_fecha);
        r.next();
        String fecha = Utils.fecha(r.getDate("PR_FECHA"));


        String sql = "insert into PVCHEQDIAENC (CE_MOVI,CE_FASE,CE_TRANSA,CE_MESA,CE_RESERVA,CE_HABI,CE_PAX,CE_FECHA,CE_MESERO," +
                "CE_ABRE_U,CE_ABRE_F,CE_ABRE_H,CE_MACHINE,CE_TURNO)"
                + " VALUES ('" + variables.movi + "'," + "'" + variables.fase + "'," + "'"
                + variables.cmd + "'," + "'" + Integer.parseInt(variables.mesa) + "'," +
                "CASE WHEN'" + variables.rva + "'='null' THEN NULL ELSE '" + variables.rva + "' END," +
                "CASE WHEN'" + variables.habi + "'='null' THEN NULL ELSE '" + variables.habi + "' END," +
                "'" + variables.mesa_pax + "',"
                + "to_date('" + fecha + "','DD/MM/YY')," + "'" + variables.mesero + "'," + "'" + variables.mesero + "',"
                + "to_date('" + fecha + "','DD/MM/YY')," + "'" + Utils.Hora(new Date()).toString() + "'," + "'" + "tablet" + "',"
                + variables.turno + ")";

        s.executeQuery(sql);
        s.close();


    }

    public void cierra_comanda_enc() throws SQLException {

        conexion = ConnectOra.getInstance().getConexion();
        String res = calcula_comanda();

        String qry_fecha = "SELECT PR_FECHA FROM PVFRONT.FRPARAM";

        Statement s = conexion.createStatement();
        ResultSet r = s.executeQuery(qry_fecha);
        r.next();
        String fecha = Utils.fecha(r.getDate("PR_FECHA"));


        String sql = "UPDATE PVCHEQDIAENC SET CE_CIERRA_F=" + "to_date('" + fecha + "','DD/MM/YYYY')," +
                "CE_CIERRA_H='" + Utils.Hora(new Date()) + "', CE_CIERRA_U='" + variables.mesero
                + "' WHERE CE_MOVI='" + variables.movi + "' AND CE_FASE= '" + variables.fase + "' AND CE_TRANSA= '" + variables.cmd + "' AND CE_MESA= '" + Integer.parseInt(variables.mesa) + "'";

        s.executeQuery(sql);
        String sqlcta="INSERT INTO PVCHEQDIACTA (CT_MOVI, CT_FASE, CT_TRANSA, CT_CUENTA, CT_IMPORTE, CT_TIP, CT_TOTAL)"+
                "VALUES ('"+variables.movi+"','"+variables.fase+"','"+variables.cmd+"','1',0,0,0)";
        s.executeQuery(sqlcta);

        String sqlfp="INSERT INTO PVCHEQDIACTAFP (FP_MOVI, FP_FASE, FP_TRANSA, FP_CUENTA, FP_RESERVA, FP_MOVI_FP, FP_FASE_FP, FP_IMPORTE, FP_FOLIO, FP_POS, FP_BANCO)"+
        "(SELECT '"+variables.movi+"','"+variables.fase+"','"+variables.cmd+"','1',NULL,PP_FP_MOVI,PP_FP_FASE,0,NULL,NULL,NULL FROM PVFPXPV WHERE PP_MOVI='"+variables.movi+"' AND PP_FASE='"+variables.fase+"' and PP_ACTIVO='S' )";
        s.executeQuery(sqlfp);
        s.close();


    }

    public void cambio_mesa(String mesa) throws SQLException {

        conexion = ConnectOra.getInstance().getConexion();
        Statement s = conexion.createStatement();
        String sql = "UPDATE PVCHEQDIAENC SET CE_MESA='" + Integer.parseInt(mesa)
                + "' WHERE CE_MOVI='" + variables.movi + "' AND CE_FASE= '" + variables.fase + "' AND CE_TRANSA= '" + variables.cmd + "' AND CE_MESA= '" + Integer.parseInt(variables.mesa) + "'";

        s.executeQuery(sql);
        s.close();


    }

    public void inserta_comanda_det(DBhelper dbhelper) throws SQLException {

        conexion = ConnectOra.getInstance().getConexion();
        SQLiteDatabase dbs = dbhelper.getWritableDatabase();


        Statement s = conexion.createStatement();
        String sql_pr = "SELECT ID,PRID,CANTIDAD,TIEMPO,NOTA,PRECIO,CARTA,COMENSAL FROM " + DBhelper.TABLE_COMANDA + " WHERE STATUS='A' AND SESION='" + variables.sesion + "' AND MESA='" + variables.mesa + "'";

        Cursor rs = dbs.rawQuery(sql_pr, null);
        if (rs.getCount() > 0) {
            rs.moveToFirst();
            do {
                String transa = genera_transa_det();


                String sql = "insert into PVCHEQDIADET (CD_MOVI, CD_FASE, CD_TRANSA, CD_ID, CD_PRODUCTO,CD_CANTIDAD, CD_PRECIO, CD_IVA, CD_IMPORTE, CD_DESCTO_IMP, CD_TOTAL, CD_TIEMPO, CD_MOD, CD_CAP_H, CD_CAP_U, CD_PROPINA_INC, CD_COMISION_INC, CD_COSTO,  CD_NOTAS,CD_CARTA,CD_RECETA,CD_CUENTA)"
                        + " VALUES ('" + variables.movi + "'," + "'" + variables.fase + "'," + "'"
                        + variables.cmd + "'," + "'" + transa + "'," + "'" + rs.getString(rs.getColumnIndex(DBhelper.CMD_PRID)) + "',"
                        + rs.getInt(rs.getColumnIndex(DBhelper.CMD_CANTIDAD)) + "," + rs.getInt(rs.getColumnIndex(DBhelper.CMD_PRECIO)) + "," + (rs.getInt(rs.getColumnIndex(DBhelper.CMD_PRECIO)) / 1.16) * .16 + "," + rs.getInt(rs.getColumnIndex(DBhelper.CMD_PRECIO)) + "," + 0 + "," + rs.getInt(rs.getColumnIndex(DBhelper.CMD_PRECIO)) + ","
                        + "'" + rs.getString(rs.getColumnIndex(DBhelper.CMD_TIEMPO)) + "'," + "'" + "0" + "'," + "'" + Utils.Hora(new Date()) + "'," + "'" + variables.mesero + "'," + 0 + "," + 0 + "," + 0 + "," +
                        "CASE WHEN'" + rs.getString(rs.getColumnIndex(DBhelper.CMD_NOTA)) + "'='null' THEN NULL ELSE '" + rs.getString(rs.getColumnIndex(DBhelper.CMD_NOTA)) + "' END," +
                        "'" + rs.getString(rs.getColumnIndex(DBhelper.CMD_CARTA)) + "','" + rs.getInt(rs.getColumnIndex(DBhelper.CMD_COMENSAL)) + "','1')";

                s.executeQuery(sql);

                inserta_modificadores_oracle(dbhelper, rs.getString(rs.getColumnIndex(DBhelper.CMD_PRID)), transa, rs.getInt(rs.getColumnIndex(DBhelper.KEY_ID)));


            } while (rs.moveToNext());
        }

        rs.close();
        s.close();

        String mac = Utils.getMACAddress("wlan0");
        mac = mac.replace(":", "");

        CallableStatement comanda = conexion.prepareCall("begin CIELOPV.COMANDERO(?,?,?,?,?,?); end;");
        comanda.setString(1, mac); // Sesion
        comanda.setString(2, variables.mesero); // usuario
        comanda.setString(3, variables.movi); // movi
        comanda.setString(4, variables.fase); // fase
        comanda.setString(5, variables.cmd); // transa
        comanda.setString(6, "TABLET"); // Hora

        comanda.execute();


    }

    public String calcula_comanda() throws SQLException {

        conexion = ConnectOra.getInstance().getConexion();
        String mac = Utils.getMACAddress("wlan0");
        mac = mac.replace(":", "");
        String res;
        CallableStatement comanda = conexion.prepareCall("{? = call  CIELOPV.CALCULA_CHEQUE(?,?,?,?,?)}");
        comanda.registerOutParameter(1, java.sql.Types.VARCHAR);
        comanda.setString(2, mac); // Sesion
        comanda.setString(3, variables.mesero); // usuario
        comanda.setString(4, variables.movi); // movi
        comanda.setString(5, variables.fase); // fase
        comanda.setString(6, variables.cmd); // transa

        comanda.execute();
        //conexion.close();
        res = comanda.getString(1);

        return res;

    }

    public void inserta_modificadores_oracle(DBhelper dbhelper, String prid, String transa, int id) throws SQLException {

        conexion = ConnectOra.getInstance().getConexion();
        SQLiteDatabase dbs = dbhelper.getWritableDatabase();

        String query = "SELECT * FROM " + DBhelper.TABLE_PVMODCG + " WHERE CG_COMANDA='"
                + variables.cmd + "'  AND CG_PRODUCTO='" + prid + "' AND CG_COMANDA_DET=" + id;

        String queryguar = "SELECT * FROM " + DBhelper.TABLE_PVGUAR + " WHERE GU_COMANDA='"
                + variables.cmd + "'  AND GU_PRODUCTO='" + prid + "' AND GU_COMANDA_DET=" + id;


        Statement s = conexion.createStatement();
        Cursor r = dbs.rawQuery(query, null);
        Cursor rs = dbs.rawQuery(queryguar, null);

        if (rs.getCount() > 0) {
            rs.moveToFirst();
            do {
                String sql = "insert into PVCHEQDIADETMODI (DM_MOVI, DM_FASE, DM_TRANSA, DM_ID, DM_MODI, DM_DESC, DM_DEFAULT, DM_VALOR)"
                        + " VALUES ('" + variables.movi + "'," + "'" + variables.fase + "'," + "'"
                        + variables.cmd + "'," + "'" + transa + "'," + "'" + rs.getString(rs.getColumnIndex(DBhelper.GU_GUAR)) + "','"
                        + rs.getInt(rs.getColumnIndex(DBhelper.GU_DESC)) + "',"
                        + "'" + rs.getString(rs.getColumnIndex(DBhelper.GU_DEFAULT)) + "',"
                        + "'" + rs.getString(rs.getColumnIndex(DBhelper.GU_SELECCION)) + "')";

                s.executeQuery(sql);
            } while (rs.moveToNext());
        }

        if (r.getCount() > 0) {
            r.moveToFirst();
            do {
                String sql = "insert into PVCHEQDIADETMODO (MP_MOVI, MP_FASE, MP_TRANSA,MP_ID,MP_GRUPO, MP_GRUPO_DESC, MP_GRUPO_MANDAT, MP_MODO, MP_MODO_DESC, MP_DEFAULT, MP_VALOR)"
                        + " VALUES ('" + variables.movi + "'," + "'" + variables.fase + "'," + "'"
                        + variables.cmd + "'," + "'" + transa + "'," + "'" + r.getString(r.getColumnIndex(DBhelper.CG_GRUPO)) + "','"
                        + r.getInt(r.getColumnIndex(DBhelper.CG_GRUPO_DESC)) + "','" + r.getInt(r.getColumnIndex(DBhelper.CG_MANDA)) + "',"
                        + "'" + r.getString(r.getColumnIndex(DBhelper.CG_MODO)) + "'," + "'" + r.getString(r.getColumnIndex(DBhelper.CG_DESC)) + "',"
                        + "'" + r.getString(r.getColumnIndex(DBhelper.CG_DEFAULT)) + "','" + r.getString(r.getColumnIndex(DBhelper.CG_SELECCION)) + "')";

                s.executeQuery(sql);
            } while (r.moveToNext());
        }
        r.close();
        rs.close();
        s.close();


    }

}
