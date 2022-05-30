import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Objects;
import java.util.Random;
import java.net.*;
import java.io.*;

public class GamePanel extends JPanel implements ActionListener {

    private final Socket clientSocket = new Socket("127.0.0.1", 8000);
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
    BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

    private static final int SCREEN_WIDTH = 600;
    private static final int SCREEN_HEIGHT = 600;
    private static final int UNIT_SIZE = 25;
    private static final int GAME_UNITS = (SCREEN_WIDTH * SCREEN_HEIGHT) / UNIT_SIZE;
    private static final int DELAY = 155; // Default - 75
    private final int[] x = new int[GAME_UNITS];
    private final int[] y = new int[GAME_UNITS];
    private int bodyParts = 6;
    private int applesEaten = 0;
    private int appleX;
    private int appleY;
    private char direction = 'R';
    private boolean running = false;
    private Timer timer;
    private final Random random;
    private int playerID = 0;
    private int enemyID;
    private int enemyScore;


    public GamePanel() throws IOException {
        random = new Random();
        this.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        this.setBackground(new Color(32, 52, 52));
        this.setFocusable(true);
        this.addKeyListener(new MyKeyAdapter());
        startGame();
        onlineGame();
    }

    public void onlineGame() throws IOException {
        playerID = Integer.parseInt(reader.readLine());
    }

    public boolean disconnect() {
        try{
            this.clientSocket.close();
            return true;
        } catch(Exception e) {
            return false;
        }
    }

    public void writeScore() throws IOException {
        writer.write(playerID + ":" + applesEaten + "\n");
        writer.flush();
    }

    public void getEnemyIdScore() throws IOException {
        String readIDScore = reader.readLine();
        String[] data = readIDScore.split(":");
        if (!Objects.equals(Integer.parseInt(data[0]), playerID)) {
            enemyID = Integer.parseInt(data[0]);
            enemyScore = Integer.parseInt(data[1]);
        }
    }

    public void startGame() {
        newApple();
        direction = 'R';
        x[0] = 0;
        y[0] = 0;

        applesEaten = 0;
        bodyParts = 6;
        running = true;
        timer = new Timer(DELAY, this);
        timer.start();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void drawScore(Graphics g) {
        g.setColor(new Color(0, 130, 130));
        g.setFont(new Font("Consolas", Font.BOLD, 30));
        FontMetrics metrics = getFontMetrics(g.getFont());
        g.drawString("Player " + playerID + ": " + applesEaten,
                (metrics.stringWidth("Player " + playerID + ": " + applesEaten) + SCREEN_WIDTH) / 30, g.getFont().getSize());
        FontMetrics metricsEnemy= getFontMetrics(g.getFont());
        g.setColor(new Color(150, 63, 124));
        g.drawString("Player " + enemyID + ": " + enemyScore,
                (metricsEnemy.stringWidth("Player " + enemyID + ": " + enemyScore) + SCREEN_WIDTH) / 2 , g.getFont().getSize());
    }

    public void draw(Graphics g) {
        if (running) {
            g.setColor(new Color(0, 160, 160));
            g.fillRect(appleX, appleY, UNIT_SIZE, UNIT_SIZE);
            for (int i = 0; i < bodyParts; i++) {
                if (i == 0) {
                    g.setColor(new Color(0, 120, 120));
                    g.fillRect(x[i], y[i], UNIT_SIZE, UNIT_SIZE);
                } else {
                    if (i % 2 == 0) {
                        g.setColor(new Color(0, 100, 100));
                    } else
                        g.setColor(new Color(0, 102, 102));
                    g.fillRect(x[i], y[i], UNIT_SIZE, UNIT_SIZE);
                }
            }
            drawScore(g);
        } else {
            gameOver(g);
        }
    }

    public void newApple() {
        appleX = random.nextInt(SCREEN_WIDTH / UNIT_SIZE) * UNIT_SIZE;
        appleY = random.nextInt(SCREEN_WIDTH / UNIT_SIZE) * UNIT_SIZE;
    }

    public void move() {
        for(int i = bodyParts; i > 0; i--) {
            x[i] = x[i - 1];
            y[i] = y[i - 1];
        }

        switch (direction) {
            case 'U' -> y[0] = y[0] - UNIT_SIZE;
            case 'D' -> y[0] = y[0] + UNIT_SIZE;
            case 'L' -> x[0] = x[0] - UNIT_SIZE;
            case 'R' -> x[0] = x[0] + UNIT_SIZE;
        }
    }

    public void checkApple() {
        if ((x[0] == appleX) && (y[0] == appleY)) {
            bodyParts++;
            applesEaten++;
            newApple();
        }
    }


    public void checkCollisions() {
        //checks if head collides with body
        for (int i = bodyParts; i > 0; i--) {
            if ((x[0] == x[i]) && (y[0] == y[i])) {
                running = false;
                break;
            }
        }
        //check if head touches left border
        if (x[0] < 0) {
            running = false;
        }
        //check if head touches right border
        if (x[0] >= SCREEN_WIDTH) {
            running = false;
        }
        //check if head touches top border
        if (y[0] < 0) {
            running = false;
        }
        //check if head touches bottom border
        if (y[0] >= SCREEN_HEIGHT) {
            running = false;
        }
        if(!running) {
            timer.stop();
        }
    }

    public void gameOver(Graphics g) {
        drawScore(g);
        g.setColor(new Color(0, 130, 130));
        g.setFont(new Font("Monospaced", Font.BOLD, 75));
        FontMetrics metrics2 = getFontMetrics(g.getFont());
        g.drawString("Game Over", (SCREEN_WIDTH - metrics2.stringWidth("Game Over"))/2, SCREEN_HEIGHT/2);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (running) {
            move();
            checkApple();
            checkCollisions();
        }
        try {
            writeScore();
            getEnemyIdScore();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        repaint();
    }

    public class MyKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            switch(e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                    if (direction != 'R'){
                        direction = 'L';
                    }
                    break;
                case KeyEvent.VK_RIGHT:
                    if (direction != 'L'){
                        direction = 'R';
                    }
                    break;
                case KeyEvent.VK_UP:
                    if (direction != 'D'){
                        direction = 'U';
                    }
                    break;
                case KeyEvent.VK_DOWN:
                    if (direction != 'U'){
                        direction = 'D';
                    }
                    break;
                case KeyEvent.VK_ENTER:
                    timer.stop();
                    startGame();
                    break;
                case KeyEvent.VK_ESCAPE:
                    disconnect();
                    System.exit(0);
            }
        }
    }

}
