package platform.client.layout;

import bibliothek.gui.*;
import bibliothek.gui.dock.*;
import bibliothek.gui.dock.event.DockFrontendAdapter;
import bibliothek.gui.dock.support.lookandfeel.LookAndFeelList;
import bibliothek.gui.dock.support.lookandfeel.ComponentCollector;
import bibliothek.gui.dock.facile.action.ReplaceActionGuard;
import bibliothek.gui.dock.control.SingleParentRemover;
import bibliothek.notes.view.menu.ThemeMenu;
import bibliothek.demonstration.util.LookAndFeelMenu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.*;
import java.io.*;

import net.sf.jasperreports.engine.*;
import platform.server.view.navigator.RemoteNavigator;
import platform.interop.UserInfo;
import platform.client.Log;
import platform.client.interop.ByteDeSerializer;
import platform.client.navigator.ClientNavigatorForm;
import platform.client.navigator.ClientNavigator;
import platform.Main;

public class Layout extends JFrame implements ComponentCollector {

    public StackDockStation DefaultStation;

    Map<String,DockStation> RootStations = new HashMap<String, DockStation>();

    DockFrontend Frontend;
    LookAndFeelList LookAndFeels;
    ThemeMenu Themes;

    public Layout(RemoteNavigator remoteNavigator) {

        setIconImage(new ImageIcon(getClass().getResource("lsfusion.gif")).getImage());
        
        UserInfo userInfo = ByteDeSerializer.deserializeUserInfo(remoteNavigator.getCurrentUserInfoByteArray());
        setTitle("LS Fusion - " + userInfo.firstName + " " + userInfo.lastName);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);

        Frontend = new DockFrontend();
        DockController Controller = Frontend.getController();

        // дизайн
        // Look&Feel'ы
        LookAndFeels = LookAndFeelList.getDefaultList();
        // пометим что мы сами будем следить за изменение Layout'а
        LookAndFeels.addComponentCollector(this);
        // темы
        Themes = new ThemeMenu(Frontend);

        // делает чтобы не удалялась основная StackForm'а
        Controller.setSingleParentRemover(new SingleParentRemover() {
            protected boolean shouldTest(DockStation dockStation) {
                return (dockStation!=DefaultStation);
            }
        });

        // можно удалять ненужные контейнеры (кроме DefaultStation)
        Controller.addActionGuard(new ReplaceActionGuard(Controller) {
            public boolean react(Dockable dockable) {
                return dockable==DefaultStation && super.react(dockable);
            }
        });
        // добавим закрытие форм
        Controller.addActionGuard(new LayoutActionGuard(Controller));

        SplitDockStation SplitStation = new SplitDockStation();

        Map<FlapDockStation,String> Flaps = new HashMap<FlapDockStation, String>();
        Flaps.put(new FlapDockStation(),BorderLayout.NORTH);
        Flaps.put(new FlapDockStation(),BorderLayout.EAST);
        Flaps.put(new FlapDockStation(),BorderLayout.SOUTH);
        Flaps.put(new FlapDockStation(),BorderLayout.WEST);

        StackDockStation StackStation = new StackDockStation();
        // the station has to be registered
        add(SplitStation, BorderLayout.CENTER);

        for(Map.Entry<FlapDockStation,String> Flap : Flaps.entrySet()) {
            add(Flap.getKey().getComponent(),Flap.getValue());
            Frontend.addRoot(Flap.getKey(),Flap.getValue()+"Flap");
        }

        Frontend.addRoot(SplitStation,"Split");

        DefaultStation = StackStation;

        ClientNavigator mainNavigator = new ClientNavigator(remoteNavigator) {

            public void openForm(ClientNavigatorForm element) {
                try {
                    Main.Layout.DefaultStation.drop(new ClientFormDockable(element.ID, this, false));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            public void openRelevantForm(ClientNavigatorForm element) {
                try {
                    if (element.isPrintForm)
                        Main.Layout.DefaultStation.drop(new ReportDockable(element.ID, this, true));
                    else
                        Main.Layout.DefaultStation.drop(new ClientFormDockable(element.ID, this, true));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        };

        NavigatorDockable mainNavigatorForm = new NavigatorDockable(mainNavigator, "Навигатор");
        // нужно обязательно до Drop чтобы появились крестики
        Frontend.add(mainNavigatorForm,"remoteNavigator");

        NavigatorDockable relevantFormNavigatorForm = new NavigatorDockable(mainNavigator.relevantFormNavigator, "Связанные формы");
        Frontend.add(relevantFormNavigatorForm,"relevantFormNavigator");

        NavigatorDockable relevantClassNavigatorForm = new NavigatorDockable(mainNavigator.relevantClassNavigator, "Классовые формы");
        Frontend.add(relevantClassNavigatorForm,"relevantClassNavigator");

        DefaultDockable logPanel = new DefaultDockable(Log.getPanel(), "Log");
        Frontend.add(logPanel, "Log");

        // нужно включить в FrontEnd чтобы была predefined и новые формы могли бы туда же попадать
        Frontend.add(StackStation,"Stack");
        Frontend.setHideable(StackStation,false);

/*        // сделаем чтобы Page'и шли без title'ов
        DockTitleFactory Factory = new NoStackTitleFactory(Controller.getTheme().getTitleFactory(Controller));
        Controller.getDockTitleManager().registerClient(StackDockStation.TITLE_ID,Factory);
        Controller.getDockTitleManager().registerClient(SplitDockStation.TITLE_ID,Factory);
        Controller.getDockTitleManager().registerClient(FlapDockStation.WINDOW_TITLE_ID,Factory);
*/
        // здесь чтобы сама потом подцепила галочки панелей
        setupMenu();

        Frontend.registerFactory(new ClientFormFactory(mainNavigator));
        try {
            read();
        } catch (IOException e) {
            SplitStation.drop(mainNavigatorForm);
            SplitStation.drop(relevantFormNavigatorForm);
            SplitStation.drop(relevantClassNavigatorForm);
            SplitStation.drop(logPanel);
            SplitStation.drop(StackStation);
        }

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e)
            {
                setVisible(false);
                try {
                    write();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

/*        String[] columnNames = {"First Name",
                                "Last Name",
                                "Sport",
                                "# of Years",
                                "Vegetarian"};

        Object[][] data = {
            {"Mary", "Campione",
             "Snowboarding", new Integer(5), new Boolean(false)},
            {"Alison", "Huml",
             "Rowing", new Integer(3), new Boolean(true)},
            {"Kathy", "Walrath",
             "Knitting", new Integer(2), new Boolean(false)},
            {"Sharon", "Zakhour",
             "Speed reading", new Integer(20), new Boolean(true)},
            {"Philip", "Milne",
             "Pool", new Integer(10), new Boolean(false)}
        };

        final JTable table = new JTable(data, columnNames);
        table.setPreferredScrollableViewportSize(new Dimension(500, 70));
        table.setFillsViewportHeight(true);

        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        final JButton button = new JButton("Test");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                JButton butt1 = new JButton();
                JButton butt2 = new JButton();
                panel.add(butt1);
                butt1.setNextFocusableComponent(table);
                table.setNextFocusableComponent(button);
//                butt1.setNextFocusableComponent(null);
                panel.remove(butt1);
//                table.setNextFocusableComponent(table);
            }
        });
        
        panel.add(table);
        panel.add(button);

        DefaultDockable dock = new DefaultDockable(panel, "Table");

        setVisible(true);

        SplitStation.drop(dock);

        for (DockableDisplayer displayer : SplitStation.getDisplayers()) {
            if (displayer instanceof BasicDockableDisplayer) {
                ((BasicDockableDisplayer)displayer).setFocusTraversalPolicy(new DefaultFocusTraversalPolicy());
            }
        }
        Container cont = table.getFocusCycleRootAncestor();
        cont.setFocusTraversalPolicy(new DefaultFocusTraversalPolicy());

        panel.setFocusCycleRoot(true);
//        panel.setFocusTraversalPolicyProvider(true);
        panel.setFocusTraversalPolicy(new DefaultFocusTraversalPolicy());

//        Container cont = table.getFocusCycleRootAncestor();
        System.out.println(cont);
        System.out.println(cont.getFocusTraversalPolicy());
        table.setNextFocusableComponent(button);
        System.out.println(cont.getFocusTraversalPolicy()); */
    }

    void write() throws IOException {
        FileOutputStream Source = new FileOutputStream("layout.txt");
        DataOutputStream out = new DataOutputStream(Source);

        LookAndFeels.write(out);
        Themes.write(out);

        out.writeInt(getExtendedState());
        setExtendedState(NORMAL);
        out.writeInt(getX());
        out.writeInt(getY());
        out.writeInt(getWidth());
        out.writeInt(getHeight());

        Frontend.write(out);

        Source.close();
    }

    void read() throws IOException{
        FileInputStream Source = new FileInputStream("layout.txt");
        DataInputStream in = new DataInputStream(Source);

        LookAndFeels.read(in);
        Themes.read(in);

        int State = in.readInt();
		setBounds(in.readInt(),in.readInt(),in.readInt(),in.readInt() );
		setExtendedState(State);

        Frontend.read(in);

        Source.close();
    }

    // настраивает меню
    void setupMenu() {
        JMenuBar Menubar = new JMenuBar();

        Menubar.add(createFileMenu());
        Menubar.add(createPanelsMenu());
        Menubar.add(createWindowMenu());
        Menubar.add(createWorkPlaceMenu());
        Menubar.add(createHelpMenu());

        setJMenuBar(Menubar);
    }

    // список компонентов которые следят за look&feel
    public Collection<Component> listComponents() {

        Set<Component> Result = new HashSet<Component>();
        for(Dockable Dockable : Frontend.listDockables())
            Result.add(Dockable.getComponent());

        for(Dockable Dockable : Frontend.getController().getRegister().listDockables())
            Result.add(Dockable.getComponent());

        Result.add(this);

        return Result;
    }

    JMenu createFileMenu() {
        // File
        JMenu Menu = new JMenu( "Файл" );
        JMenuItem OpenReport = new JMenuItem("Открыть отчет...");
        OpenReport.setToolTipText("Открывает ранее сохраненный отчет");
        final JFrame MainFrame = this;
        OpenReport.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                FileDialog Chooser = new FileDialog(MainFrame,"Отчет");
                Chooser.setFilenameFilter(new FilenameFilter(){
                    public boolean accept(File directory, String file) {
                        String filename = file.toUpperCase();
                        return filename.endsWith(".JRPRINT");
                    }
                });
                Chooser.setVisible(true);

                try {
                    DefaultStation.drop(new ReportDockable(Chooser.getFile(),Chooser.getDirectory()));
                } catch (JRException e1) {
                    e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        });
        Menu.add(OpenReport);
        return Menu;
    }

    JMenu createPanelsMenu() {
        // панели считаем с Frontendа
        JMenu Menu = new JMenu("Панели");
		for(final Dockable Dockable : Frontend.listDockables()){
            if(Dockable.asDockStation()==null && Frontend.isHideable(Dockable)) {
                final JCheckBoxMenuItem Item = new JCheckBoxMenuItem(Dockable.getTitleText());
                Item.setSelected(true);
                Item.addActionListener(new ActionListener() {
	        		public void actionPerformed(ActionEvent e) {
        				if(Item.isSelected())
		        			Frontend.show(Dockable);
        				else
        					Frontend.hide(Dockable);
           			}
    		    });

        		Frontend.addFrontendListener(new DockFrontendAdapter(){
    		        public void hidden(DockFrontend Frontend,Dockable Affected) {
	    		    	if(Affected==Dockable)
		    		    	Item.setSelected( false );
    			    }

    	    		public void shown(DockFrontend Frontend,Dockable Affected) {
	    	    		if(Affected==Dockable)
		    	    		Item.setSelected(true);
			        }
                });

                Menu.add(Item);
            }
        }
        return Menu;
    }

    JMenu createWindowMenu() {
        JMenu Menu = new JMenu( "Окно" );
		Menu.add(new LookAndFeelMenu(this,LookAndFeels));
        // темы делаем
        Menu.add(Themes);
        return Menu;
    }

    Map<String,JRadioButtonMenuItem> WorkPlaces = new HashMap<String, JRadioButtonMenuItem>();
    JMenu WorkPlaceMenu = new JMenu("АРМ");
    Map<String,JMenuItem> RemoveWorkPlaces = new HashMap<String, JMenuItem>();
    JMenu RemoveWorkPlaceMenu = new JMenu("Удалить");

    JMenu createWorkPlaceMenu() {
        JMenuItem Save = new JMenuItem("Сохранить");
		Save.addActionListener( new ActionListener(){
			public void actionPerformed(ActionEvent e) {
                String Name = Frontend.getCurrentSetting();
                if(Name==null)
                    Name = "АРМ " + (Frontend.getSettings().size()+1);
				Frontend.save(Name);
			}
		});
        WorkPlaceMenu.add(Save);

        final JFrame MainFrame = this;
        JMenuItem SaveAs = new JMenuItem("Сохранить как...");
        SaveAs.addActionListener( new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                String Name = JOptionPane.showInputDialog(MainFrame,"Название АРМ :","АРМ "+(Frontend.getSettings().size()+1));
                if(Name!=null)
                    Frontend.save(Name);
            }
        });
        WorkPlaceMenu.add(SaveAs);
        WorkPlaceMenu.add(RemoveWorkPlaceMenu);

        Frontend.addFrontendListener(new DockFrontendAdapter(){
            public void read(DockFrontend Frontend,String Name) {
                // считали св-во
                createWorkPlaceLoadItem(Name);
                if(Frontend.getCurrentSetting().equals(Name))
                    selectItem(Name);
            }

            void selectItem(String Name) {
                for(Map.Entry<String,JRadioButtonMenuItem> WorkPlace : WorkPlaces.entrySet())
                    WorkPlace.getValue().setSelected(false);
                createWorkPlaceLoadItem(Name).setSelected(true);
            }

            public void saved(DockFrontend Frontend,String Name) {
                // сохранили св-во
                createWorkPlaceLoadItem(Name);
                selectItem(Name);
            }

            public void loaded(DockFrontend Frontend,String Name) {
                selectItem(Name);
            }

            public void deleted(DockFrontend Frontend,String Name) {

                // нужно удалить
                WorkPlaceMenu.remove(WorkPlaces.get(Name));
                WorkPlaces.remove(Name);
                RemoveWorkPlaceMenu.remove(RemoveWorkPlaces.get(Name));
                RemoveWorkPlaces.remove(Name);
            }
        });

        return WorkPlaceMenu;
    }

	JRadioButtonMenuItem createWorkPlaceLoadItem(final String Name){
        JRadioButtonMenuItem Item = WorkPlaces.get(Name);
        if(Item==null) {
            Item = new JRadioButtonMenuItem(Name);
            Item.addActionListener( new ActionListener(){
		    	public void actionPerformed(ActionEvent e) {
		    		Frontend.load(Name);
			    }
    		});
            if(WorkPlaces.size()==0)
                WorkPlaceMenu.addSeparator();
            WorkPlaces.put(Name,Item);
            WorkPlaceMenu.add(Item);

            JMenuItem RemoveItem = new JMenuItem(Name);
            RemoveItem.addActionListener( new ActionListener(){
		    	public void actionPerformed(ActionEvent e) {
		    		Frontend.delete(Name);
			    }
    		});

            RemoveWorkPlaces.put(Name,RemoveItem);
            RemoveWorkPlaceMenu.add(RemoveItem);
        }
        return Item;
	}

    JMenu createHelpMenu() {
        JMenu Menu = new JMenu("Справка");
        final JMenuItem About = new JMenuItem("О программе");
        About.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               }
        });
        Menu.add(About);
        return Menu;
    }
}

/*
class Log {

    void S
}

class Status {

}
*/