package View;

import java.util.Scanner;
import Controller.*;
import Model.*;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.util.List;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

public class Juego {

    public Renderizador rend;
    public InterpreteComandos lector;
    public GestorMapas gestorMapa;
    public Personaje p1;
    public Personaje p2;
    private final Scanner scan;
    public int nivel;
    public boolean inicio_Nivel;
    public List<String> lstMenu;
    public List<String> lstInstrucciones;    
    
    private Ventana ventana;
    
    public static final int MOSTRAR_INSTRUCCIONES = -1;
    public static final int MENU_JUEGO = 0;
    public static final int NOMBRE_PLAYER1 = 1;
    public static final int NOMBRE_PLAYER2 = 2;
    public static final int CAPTURAR_MOVIMIENTO = 3;
    public static final int CAPTURAR_ACCION_ESPECIAL = 4;
    public static final int CAPTURAR_ACCION_DUO = 5;
    public static final int NO_CAPTURAR = 9;

    public static int eventFlag = MENU_JUEGO; // INDICA EN QUE SECCION ESTOY
    
    private Animacion animacion;
    
    public Juego(Ventana ventana) {
        rend = new Renderizador();
        lector = new InterpreteComandos();
        gestorMapa = new GestorMapas();
        scan = new Scanner(System.in);
        p1 = p2 = null;
        nivel = 0;
        inicio_Nivel = true;
        this.ventana = ventana;
        this.inicializarPersonajes(nivel);
        this.cargar_Dialogos();
        this.animacion = new Animacion(ventana);
        this.animacion.start();
    }

    public void capturarAccion(char key) throws IOException, InterruptedException {
        Mapa m = this.gestorMapa.getMapa(nivel);
        if (m == null){
            System.out.println("Error mapa nulo");
            System.exit(0);
        }
        
        int resultado = lector.interpretaMovimiento(key, p1, p2, m, nivel, animacion);
        animacion.iniciar(resultado);
        
        
    }

    public int caputarAccionEspecial(char key, int index) throws IOException, InterruptedException{
        /*0-> SE FALLÓ*/
        /*1-> EXITO TOTAL, ACCION P1*/
        /*2-> EXITO TOTAL, ACCION P2*/
        /*3-> EXITO TOTAL, ACCION DUO*/
        /*4-> EXITO PARCIAL*/
        int tipo;
        tipo = lector.interpretaAccionEspecial(key, index, gestorMapa.getMapa(nivel), p1, p2, nivel);
        if (tipo == 0){
            //RESTAR VIDA PERSONAJE
            p1.setVida(p1.getVida() - 2);
            this.rend.pnlTexto_mostrarDatos(ventana.pnlTexto, p1, p2);
        }else if (tipo == 1 || tipo == 2 || tipo == 3){
            ventana.pnlTexto.getGraphics().clearRect(0, 0, ventana.pnlTexto.getWidth(), ventana.pnlTexto.getHeight());
            ejecutarAccionEspecial(tipo);
            eventFlag = CAPTURAR_MOVIMIENTO;
            p1.actualizarEstado(gestorMapa.getMapa(nivel));;
            p2.actualizarEstado(gestorMapa.getMapa(nivel));;   
        }
        return tipo;
    }
    
    private void ejecutarAccionEspecial(int player) throws IOException, InterruptedException{
        /*PLAYER INDICA QUE JUGADOR MANDÓ LA ACCION*/
        if (nivel == 0) {
            if (player == 1) {
                for (int i = 0; i < 3; i++) {
                    p1.setPosX(p1.getPosX() + 1);
                    this.renderizar();
                    Thread.sleep(750);
                }
                //ACTIVAR TERRENOS
                //IMPLEMENTAR
            } else if (player == 2) {
                //NOTHING
            } else if (player == 3) {
                p1.setPosY(p1.getPosY() + 1);
                p2.setPosY(p2.getPosY() - 1);
                this.renderizar();
                Thread.sleep(750);
                for (int i = 0; i < 2; i++) {
                    p1.setPosX(p1.getPosX() + 1);
                    p2.setPosX(p2.getPosX() + 1);
                    this.renderizar();
                    Thread.sleep(750);
                }
            }
        } else if (nivel == 1) {
            if (player == 1) {
                int xOrig = p1.getPosX();
                int yOrig = p1.getPosY();
                for (int i = 0; i < 2; i++) {
                    p1.setPosY(p1.getPosY() + 1);
                    this.renderizar();
                    Thread.sleep(750);
                }
                p1.setPosY(yOrig + 4);
                this.renderizar();
                Thread.sleep(750);
                //AQUI DESTRUYE ESAS COSAS
                Celda celda1 = gestorMapa.getMapa(nivel).getMapaAt(yOrig + 4, xOrig);
                Celda celda2 = gestorMapa.getMapa(nivel).getMapaAt(yOrig + 5, xOrig);
                try{
                    Dibujable d = GestorXML.ObtenerDibujable('N',0,0);
                    celda1.setObj(d);
                    this.renderizar();
                    Thread.sleep(750);
                    celda2.setObj(d);
                    this.renderizar();
                    Thread.sleep(750);
                }catch(Exception ex){}
                //VUELVE AL ORIGINAL
                p1.setPosX(xOrig);
                p1.setPosY(yOrig);
                this.renderizar();
                Thread.sleep(750);
                //ACTIVAR TERRENOS
                //IMPLEMENTAR
            } else if (player == 2) {
                //RECORRE TERRITORIO
                int xOrig = p2.getPosX();
                int yOrig = p2.getPosY();
                p2.setPosX(xOrig - 1);
                this.renderizar();
                Thread.sleep(750);
                /////
                p2.setPosX(xOrig - 2);
                this.renderizar();
                Thread.sleep(750);
                //DESTRUYE LA ARENA
                Celda celda = gestorMapa.getMapa(nivel).getMapaAt(yOrig, xOrig - 1);
                celda.setObj(new Terreno('N', 2));
                try{
                    Dibujable d = GestorXML.ObtenerDibujable('N',0,0);
                    celda.setObj(d);
                    this.renderizar();
                    Thread.sleep(750);
                }catch(Exception ex){}
            } else if (player == 3) {
                //NOTHING
            }
        } else if (nivel == 2) {
            if (player == 1) {
                for (int i = 0; i < 3; i++) {
                    p1.setPosX(p1.getPosX() + 1);
                    this.renderizar();
                    Thread.sleep(750);
                }
                //ACTIVAR TERRENOS
                //IMPLEMENTAR
            } else if (player == 2) {
                //NOTHING
            } else if (player == 3) {
                p1.setPosX(p1.getPosX() + 1);
                p2.setPosX(p2.getPosX() + 1);
                this.renderizar();
                Thread.sleep(750);
                p1.setPosX(p1.getPosX() + 3);
                p1.setPosY(p1.getPosY() - 1);
                p2.setPosX(p2.getPosX() + 3);
                this.renderizar();
                Thread.sleep(750);
                p1.setPosY(p1.getPosY() + 1);
                p2.setPosY(p2.getPosY() - 1);
                this.renderizar();
                Thread.sleep(750);
            }
        } else if (nivel == 3) {//NIVEL CON ENEMIGO
            if (player == 1) {
                //NADA
            } else if (player == 2) {
                //ANIMACION
                int xOrig = p2.getPosX();
                int yOrig = p2.getPosY();
                //1
                p2.setPosY(yOrig - 1);
                p2.setPosX(xOrig - 1);
                this.renderizar();
                Thread.sleep(750);
                //2
                p2.setPosY(yOrig - 3);
                this.renderizar();
                Thread.sleep(750);
                //3
                p2.setPosY(yOrig - 4);
                this.renderizar();
                Thread.sleep(750);
                //DESTRUYE ENEMIGO Y TRIGGERS
                try{
                    Dibujable dib = GestorXML.ObtenerDibujable('S', 0, 0);
                    Terreno t = null;
                    if (dib instanceof Terreno) 
                        t = (Terreno) dib;
                    Mapa m = gestorMapa.getMapa(nivel);
                    m.setMapaAt(4, 9, t);
                    m.setMapaAt(3, 10, t);
                    m.setMapaAt(4, 10, t);
                    m.setMapaAt(5, 10, t);
                    m.setMapaAt(6, 10, t);
                }catch(Exception ex3){}
                //4
                p2.setPosX(xOrig);
                p2.setPosY(yOrig);
                this.renderizar();
                Thread.sleep(750);
            } else if (player == 3) {
                //NOTHING
            }
        }
    }

    public void actualizarInfo() throws IOException, InterruptedException {
        int posX1 = p1.getPosX();
        int posY1 = p1.getPosY();
        int posX2 = p2.getPosX();
        int posY2 = p2.getPosY();

        Mapa mapa = this.gestorMapa.getMapa(nivel);
        Dibujable obj1 = mapa.getMapaAt(posY1, posX1).getObj();
        Dibujable obj2 = mapa.getMapaAt(posY2, posX2).getObj();
        
        /*CAMBIA FLAGS*/
        String mensaje = null;
        if (obj1 instanceof Terreno)
            if (((Terreno) obj1).getTipo() == 3 && ((Terreno) obj1).getActivo()) {
                
                mensaje = "CAISTE EN CELDA";
                ventana.pnlTexto.getGraphics().drawString(mensaje, 15, 95);
                mensaje = "DE ACCION ESPECIAL!";
                ventana.pnlTexto.getGraphics().drawString(mensaje, 15, 110);
                mensaje = "Ingrese la accion: " + p1.getAccionEspecial(nivel);
                ventana.pnlTexto.getGraphics().drawString(mensaje, 15, 150);                    
                eventFlag = CAPTURAR_ACCION_ESPECIAL;
            }
        if (obj2 instanceof Terreno)
            if (((Terreno) obj2).getTipo() == 3 && ((Terreno) obj2).getActivo()) {
                mensaje = "CAISTE EN CELDA";
                ventana.pnlTexto.getGraphics().drawString(mensaje, 15, 95);
                mensaje = "DE ACCION ESPECIAL!";
                ventana.pnlTexto.getGraphics().drawString(mensaje, 15, 110);
                mensaje = "Ingrese la accion: " + p2.getAccionEspecial(nivel);
                ventana.pnlTexto.getGraphics().drawString(mensaje, 15, 150);        
                eventFlag = CAPTURAR_ACCION_ESPECIAL;
            }
            else if (obj1 instanceof Terreno)
                if (((Terreno) obj1).getTipo() == 4
                        && ((Terreno) obj2).getTipo() == 4
                        && ((Terreno) obj1).getActivo()
                        && ((Terreno) obj2).getActivo()) {
                    
                    mensaje = "CAISTE EN CELDA";
                    ventana.pnlTexto.getGraphics().drawString(mensaje, 15, 95);
                    mensaje = "DE ACCION DUO!";
                    ventana.pnlTexto.getGraphics().drawString(mensaje, 15, 110);
                    mensaje = "Ingrese la accion: " + p1.getAccionDuo(nivel);
                    ventana.pnlTexto.getGraphics().drawString(mensaje, 15, 150);                       
                    
                    eventFlag = CAPTURAR_ACCION_DUO;
                }     

        /*VERIFICA LLEGARON A LA META*/
        if (obj1 instanceof Terreno && obj2 instanceof Terreno) {
            if (((Terreno)  obj1).getTipo() == 6
                    && ((Terreno) obj2).getTipo() == 6) {
                renderizar();
                nivel += 1;
                inicio_Nivel = true;
                Thread.sleep(1000);
                if (nivel < gestorMapa.getNumNiveles()){
                    inicializarPersonajes(nivel);
                    restaurarActividad(nivel);
                    renderizar();
                }else{
                    inicializarPersonajes(0);
                    restaurarActividad(0);
                } 
            }
        }
        
        /*VERIFICA SI EL PERSONAJE CAYÓ EN UN TRIGGER ENEMIGO*/
        if (obj1 instanceof Terreno) {
            Terreno ter = (Terreno) obj1;
            if (ter.getActivo() && ter.getTipo() == 5) {
                //ACTIVE LA VISIBILIDAD DEL ENEMIGO Y QUE LO MUESTRE
                Enemigo e = this.gestorMapa.getEnemigo(nivel);
                e.setElementoGrafico('E');
                //DISMINUIR LA VIDA DEL JUGADOR 1
                //this.p1.setVida(this.p1.getVida() - 1);
                //ACTIVAR TERRENOS
                //IMPLEMENTAR
            }
        }
        
        /*VERIFICAR FIN DE JUEGO*/
        if (finJuego()){
            //cambia eventFlag -> MENU
            eventFlag = MENU_JUEGO;
            p1.setVida(10);
            if (nivel == gestorMapa.getNumNiveles())
                nivel = 0;
            this.inicializarPersonajes(nivel);
            this.restaurarActividad(nivel);
            ventana.mostrarMenu();
        }
    }

    public void renderizar() throws IOException, InterruptedException {
        Mapa mapa = this.gestorMapa.getMapa(nivel);
        Graphics graph = ventana.pnlGrafico.getGraphics();
        rend.dibujarMapa(graph, mapa);
        rend.dibujarJugadores(graph, p1, p2);
        rend.pnlTexto_mostrarDatos(ventana.pnlTexto, p1, p2);
    }   
    //NEW - TESTING
    public void dibujarMovJugador() throws IOException, InterruptedException{
        Mapa mapa = this.gestorMapa.getMapa(nivel);
        Graphics graph = ventana.pnlGrafico.getGraphics();
        rend.dibujarMovJugador(graph,mapa,p1,p2);
        rend.pnlTexto_mostrarDatos(ventana.pnlTexto, p1, p2);
    }
    /**/
    private boolean finJuego() {
        /*TOPE NIVEL: cantidad de mapas*/
        /*Si ha muerto o terminó todos los niveles*/
        return (p1.getVida() <= 0) || (nivel == gestorMapa.getNumNiveles());
    }

    private void inicializarPersonajes(int nivel) {
        /*AQUI SE PUEDE REALIZAR LECTURA DE PERSONAJE Y ENEMIGO*/
        /*SUS DATOS, ETC*/
        if (nivel < 0 || nivel >= gestorMapa.getNumNiveles()) return;
        if (this.p1 == null) this.p1 = new Personaje('A');
        if (this.p2 == null) this.p2 = new Personaje('B');
        if (this.p1.getVida() <= 0) this.p1.setVida(10);
        //Cargar datos de posición y acciones de los personajes
        Cargar_Personajes_XML(nivel);
        //Cargar imágenes de los personajes
        try {
            this.p1.setImagen("./Resources/sprite_cuy.png");
            this.p2.setImagen("./Resources/sprite_perro.png");
        } catch (IOException ex) {
            System.err.println("ERROR: No se pudieron cargar las imagenes de los personajes.");
            System.exit(1);
        }
    }

    private void Cargar_Personajes_XML(int nivel) {
        try {
            File inputFile = new File("./Files/personajes.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("Nivel");
            Node nNode = nList.item(nivel);
            Element eElement = (Element) nNode;
            
            p1.setPosY(Integer.parseInt(eElement.getElementsByTagName("p1PosY").item(0).getTextContent()));
            p1.setPosX(Integer.parseInt(eElement.getElementsByTagName("p1PosX").item(0).getTextContent()));
            p1.setAccionEspecial(eElement.getElementsByTagName("p1AccEsp").item(0).getTextContent(), nivel);
            p1.setOldX(p1.getPosX());
            p1.setOldY(p1.getPosY());
            
            p2.setPosY(Integer.parseInt(eElement.getElementsByTagName("p2PosY").item(0).getTextContent()));
            p2.setPosX(Integer.parseInt(eElement.getElementsByTagName("p2PosX").item(0).getTextContent()));
            p2.setAccionEspecial(eElement.getElementsByTagName("p2AccEsp").item(0).getTextContent(), nivel);
            p2.setOldX(p2.getPosX());
            p2.setOldY(p2.getPosY());
            
            p1.setAccionDuo(eElement.getElementsByTagName("AccDuo").item(0).getTextContent(), nivel);
            p2.setAccionDuo(eElement.getElementsByTagName("AccDuo").item(0).getTextContent(), nivel);
        } catch (FileNotFoundException ex) {
            System.err.println("ERROR: No se ha encontrado el archivo personajes.xml");
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }   
   
    private void cargar_Actividad_XML(int nivel) {
        Mapa mapa = gestorMapa.getMapa(nivel);        
        try {
            File inputFile = new File("./Files/terreno.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("Nivel");
            Node nNode = nList.item(nivel);
            Element eElement = (Element) nNode;
            
            NodeList filas = eElement.getElementsByTagName("fila");
            NodeList columnas = eElement.getElementsByTagName("columna");
            
            for (int i = 0; i < filas.getLength(); i++){
                int fila = Integer.parseInt(filas.item(i).getTextContent());
                int col = Integer.parseInt(columnas.item(i).getTextContent());
                Terreno terreno = ((Terreno) mapa.getMapaAt(fila, col).getObj());
                terreno.setActivo(false);
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }    
    
    private void restaurarActividad(int nivel){
        Mapa mapa = gestorMapa.getMapa(nivel);
        if (mapa == null) return;
        for (int i = 0; i < 12; i++)
            for(int j = 0; j < 16; j++){
                Celda celda = mapa.getMapaAt(i, j);
                Dibujable dib = celda.getObj();
                if (dib instanceof Terreno)
                    ((Terreno) dib).setActivo(true);
            }
    }
    
    private void cargar_Dialogos() {
        BufferedReader br = null;

        lstMenu = new ArrayList<String>();
        leer_Arch_Lineas(br, "./Files/Dialogo0.txt", lstMenu);

        lstInstrucciones = new ArrayList<String>();
        leer_Arch_Lineas(br, "./Files/Dialogo1.txt", lstInstrucciones);

    }

    private void leer_Arch_Lineas(BufferedReader br, String ruta, List<String> lstString) {
        try {
            br = new BufferedReader(new FileReader(ruta));
            String linea;
            while ((linea = br.readLine()) != null) {
                lstString.add(linea);
            }
        } catch (FileNotFoundException ex) {
            System.err.println("ERROR: No se encontro el archivo " + ruta);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }        
    
    //#MOD
    public void MostrarAccionesPantalla() throws IOException{
        BufferedImage[] imagenes = new BufferedImage[4];
        for (int i = 0; i < 4; i++){
            imagenes[i] = ImageIO.read(new File("./Transiciones/"+(i+1)+".png"));
        }
        //CARGAR XML
        try {
            File inputFile = new File("./Transiciones/transiciones.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("Nivel");
            ////
            
            Node nNode = nList.item(nivel);
            Element eElement = (Element) nNode;
            //ACCION ESPECIAL PLAYER1
            NodeList listAcEsp =  eElement.getElementsByTagName("accionEspecial");
            Element acEsp1 = (Element) listAcEsp.item(0);
            NodeList posX = acEsp1.getElementsByTagName("posX");
            NodeList posY = acEsp1.getElementsByTagName("posY");
            for (int i = 0; i < posX.getLength(); i++){
                int x = Integer.parseInt(posX.item(i).getTextContent());
                int y = Integer.parseInt(posY.item(i).getTextContent());
                Graphics g = ventana.pnlGrafico.getGraphics();
                g.drawImage(imagenes[i], x*64, y*64, null);
            }
            Element acEsp2 = (Element) listAcEsp.item(1);
            posX = acEsp2.getElementsByTagName("posX");
            posY = acEsp2.getElementsByTagName("posY");
            for (int i = 0; i < posX.getLength(); i++){
                int x = Integer.parseInt(posX.item(i).getTextContent());
                int y = Integer.parseInt(posY.item(i).getTextContent());
                Graphics g = ventana.pnlGrafico.getGraphics();
                g.drawImage(imagenes[i], x*64, y*64, null);
            }
            //ACCION DUO PLAYER1
            NodeList listAcDuo =  eElement.getElementsByTagName("accionDuo");
            Element acDuo1 = (Element) listAcDuo.item(0);
            posX = acDuo1.getElementsByTagName("posX");
            posY = acDuo1.getElementsByTagName("posY");
            for (int i = 0; i < posX.getLength(); i++){
                int x = Integer.parseInt(posX.item(i).getTextContent());
                int y = Integer.parseInt(posY.item(i).getTextContent());
                Graphics g = ventana.pnlGrafico.getGraphics();
                g.drawImage(imagenes[i], x*64, y*64, null);
            }
            //ACCION DUO PLAYER2
            Element acDuo2 = (Element) listAcDuo.item(1);
            posX = acDuo2.getElementsByTagName("posX");
            posY = acDuo2.getElementsByTagName("posY");
            for (int i = 0; i < posX.getLength(); i++){
                int x = Integer.parseInt(posX.item(i).getTextContent());
                int y = Integer.parseInt(posY.item(i).getTextContent());
                Graphics g = ventana.pnlGrafico.getGraphics();
                g.drawImage(imagenes[i], x*64, y*64, null);
            }

        } catch (Exception e) {
            //e.printStackTrace();
        }
        
    }
}
