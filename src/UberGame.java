import java.applet.Applet;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.filechooser.FileFilter;

public class UberGame extends Applet {
	private static final long serialVersionUID = 1L;

	public File saveFile = new File("image.png");
	public class pngFile extends FileFilter {
		public boolean accept(File f) {
			return f.getName().toLowerCase().endsWith(".png");
		}

		public String getDescription() {
			return "PNG Image (*.png)";
		}
	};
	public BufferedImage currImage = new BufferedImage(600, 500, BufferedImage.TYPE_INT_RGB);
	public Graphics2D gCurrImage = currImage.createGraphics();
	public BufferedImage newImage = new BufferedImage(600, 500, BufferedImage.TYPE_INT_RGB);
	public Graphics2D gNewImage = newImage.createGraphics();

	public Particle[][] particleMap = new Particle[600][500];
	public int[] biasCache;

	public Particle[] particles = {
		new Particle("Empty", 0, new Color(0, 0, 0), false, true, 0, 0, 0),
		new Particle("Wall", 1, new Color(100, 100, 100), false, false, 0, 10, 100),
		new Particle("Sand", 2, new Color(255, 222, 173), true, true, 0, 0, 60),
		new Particle("Water", 3, new Color(0, 0, 255), true, true, 30, 10, 40),
		new Particle("Oil", 4, new Color(155, 122, 73), true, true, 20, 8, 30)
		//new Particle("Fire", 5, new Color(255, 50, 0), true, true, 5, 0, 0)
	};

	public boolean mouseDown = false;
	public boolean boundsOn = true;
	public boolean shiftDown = false;
	public boolean ctrlDown = false;
	public boolean gamePaused = false;
	public Point startPoint;
	public int mouseX, mouseY;
	public int selectedType = 2;
	public int penSize = 20;
	public boolean isInApplet = false;

	public static UberGame game = null;
	public Image logo1 = null;

	/**
	 * This method is called when run as an application
	 * @param args
	 */
	public static void main(String[] args) {
		game = new UberGame();
		UberSand applet = game.new UberSand(false);
		applet.init();
		applet.start();
	}

	/**
	 * Sets the size of the applet in the browser (loader applet)
	 */
	public void init() {
		setSize(300, 200);
	}

	/**
	 * This method is called when run as an applet
	 */
	public void start() {
		game = this;
		//logo1 = Toolkit.getDefaultToolkit().createImage("../logo1.png");
		UberSand applet = new UberSand(true);
		applet.init();
		applet.start();
		new Painter(this, 5000);
	}

	public void paint(Graphics g) {
		update(g);
	}

	/**
	 * Paints the logo on the loader applet
	 */
	public void update(Graphics g) {
		if (logo1 != null) {
			g.drawImage(logo1, 0, 0, this);
		} else {
			g.setColor(Color.BLACK);
			g.setFont(new Font("Arial Black", Font.BOLD, 64));
			g.drawString("Uber", 50, 70);
			g.drawString("Sand", 50, 140);
		}
	}

	public static int random(int a, int b) {
		return (int) (Math.random() * (b - a + 1)) + a;
	}

	class UberSand extends Applet implements MouseListener, MouseWheelListener, MouseMotionListener, KeyListener, ActionListener {
		private static final long serialVersionUID = 1L;

		/**
		 * Create the JFrame for the applet to be viewed in.
		 */
		public UberSand(boolean applet) {
			clear();
			JFrame theGUI = new JFrame("Uber Sand");
			if (!applet) theGUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			Container pane = theGUI.getContentPane();
			pane.setLayout(null);
			pane.setSize(600, 655);
			pane.setPreferredSize(new Dimension(600, 655));
			pane.setBackground(new Color(100, 100, 100));
			this.setBounds(0, 0, 600, 500);
			pane.add(this);

			// Add the menu and buttons
			JLabel l1 = new JLabel("Menu");
			l1.setForeground(Color.WHITE);
			l1.setBounds(25, 515, 150, 15);
			pane.add(l1);
			int lastx = 20;
			for (Particle par : particles) {
				JButton button = new JButton(par.name + " (" + par.type + ")");
				button.setActionCommand("type" + par.name);
				button.addActionListener(this);
				Dimension size = button.getPreferredSize();
				button.setBounds(lastx, 540, size.width, size.height);
				lastx = button.getX() + button.getWidth();
				pane.add(button);
			}

			JButton b1 = new JButton("Clear (C)");
			b1.setActionCommand("clear");
			b1.addActionListener(this);
			Dimension size = b1.getPreferredSize();
			b1.setBounds(20, 570, size.width, size.height);
			pane.add(b1);
			JButton b2 = new JButton("Toggle Bounds (B)");
			b2.setActionCommand("toggleBounds");
			b2.addActionListener(this);
			size = b2.getPreferredSize();
			b2.setBounds(b1.getX() + b1.getWidth(), 570, size.width, size.height);
			pane.add(b2);
			JButton b3 = new JButton("Pause Game (P)");
			b3.setActionCommand("pauseGame");
			b3.addActionListener(this);
			size = b3.getPreferredSize();
			b3.setBounds(b2.getX() + b2.getWidth(), 570, size.width, size.height);
			pane.add(b3);
			JButton b4 = new JButton("Save to File");
			b4.setActionCommand("saveImage");
			b4.addActionListener(this);
			size = b4.getPreferredSize();
			b4.setBounds(20, 600, size.width, size.height);
			pane.add(b4);
			JButton b5 = new JButton("Load from File");
			b5.setActionCommand("loadImage");
			b5.addActionListener(this);
			size = b5.getPreferredSize();
			b5.setBounds(b4.getX() + b4.getWidth(), 600, size.width, size.height);
			pane.add(b5);
			JButton b6 = new JButton("Exit");
			b6.setActionCommand("exitGame");
			b6.addActionListener(this);
			size = b6.getPreferredSize();
			b6.setBounds(b3.getX() + b3.getWidth(), 570, size.width, size.height);
			pane.add(b6);

			// Finish with the JFrame
			theGUI.pack();
			theGUI.setResizable(false);
			theGUI.setVisible(true);
		}

		public void start() {
			addMouseListener(this);
			addMouseMotionListener(this);
			addKeyListener(this);
			addMouseWheelListener(this);
			new Painter(this, 10);
			new BiasUpdater(this, 1000);
			//for (int i = 0; i < 600; i += 20) {
			//	new Computer(this, i, 1);
			//}
		}

		public void paint(Graphics g) {
			update(g);
		}

		public void clear() {
			gNewImage.setColor(particles[0]);
			gNewImage.fillRect(0, 0, 600, 500);
			computeMap(0, 0, 600, 500);
		}

		public void update(Graphics g) {
			if (mouseDown && !shiftDown && !ctrlDown) {
				gNewImage.setColor(particles[selectedType]);
				if (penSize > 1) {
					gNewImage.fillOval(mouseX - (penSize / 2), mouseY - (penSize / 2), penSize, penSize);
				} else {
					gNewImage.fillRect(mouseX, mouseY, 1, 1);
				}
				computeMap(mouseX - (penSize / 2), mouseY - (penSize / 2), mouseX + (penSize / 2), mouseY + (penSize / 2));
			}
			if (!gamePaused) computeMove();
			gCurrImage.drawImage(newImage, 0, 0, this);
			if (mouseDown && shiftDown) {
				gCurrImage.setColor(particles[selectedType]);
				gCurrImage.setStroke(new BasicStroke(penSize - 1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				gCurrImage.drawLine(startPoint.x, startPoint.y, mouseX, mouseY);
			} else if (mouseDown && ctrlDown) {
				gCurrImage.setColor(particles[selectedType]);
				gCurrImage.drawRect(startPoint.x, startPoint.y, mouseX - startPoint.x - 1, mouseY - startPoint.y - 1);
			} else if (isInApplet) {
				gCurrImage.setColor(Color.WHITE);
				gCurrImage.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				gCurrImage.drawOval(mouseX - (penSize / 2), mouseY - (penSize / 2), penSize, penSize);
			}
			g.drawImage(currImage, 0, 0, this);
		}

		/**
		 * Compute the particleMap cache
		 */
		public void computeMap(int x, int y, int x2, int y2) {
			int nx = Math.max(Math.min(x, x2) - 1, 0);
			int ny = Math.max(Math.min(y, y2) - 1, 0);
			int nx2 = Math.min(Math.max(x, x2) + 1, 600);
			int ny2 = Math.min(Math.max(y, y2) + 1, 500);
			for (int x3 = nx; x3 < nx2; x3++) {
				for (int y3 = ny; y3 < ny2; y3++) {
					int rgb = newImage.getRGB(x3, y3);
					for (Particle par : particles) {
						if (par.getRGB() == rgb) {
							particleMap[x3][y3] = par;
							break;
						}
					}
				}
			}
		}

		/**
		 * Compute physics movements
		 */
		public void computeMove() {
			if (biasCache == null) computeBias();
			for (int y = 499; y >= 0; y--) {
				for (int i = 0; i < biasCache.length; i++) {
					int x = biasCache[i];
					if (particleMap[x][y].gravity) {
						particleMap[x][y].compute(x, y, boundsOn, newImage);
					}
				}
			}
		}

		public void computeBias() {
			int[] biasCache = new int[600];
			for (int i = 0; i < biasCache.length; i++) {
				biasCache[i] = i;
			}
			for (int i = 0; i < biasCache.length * 2; i++) {
				int a = random(0, biasCache.length - 1);
				int b = random(0, biasCache.length - 1);
				if (a != b) {
					int tmp = biasCache[a];
					biasCache[a] = biasCache[b];
					biasCache[b] = tmp;
				}
			}
			UberGame.this.biasCache = biasCache;
		}

		public void loadFromFile(File file) {
			try {
				BufferedImage tempImage = ImageIO.read(file);
				clear();
				if (tempImage.getWidth() == 600 && tempImage.getHeight() == 500) {
					for (int x = 0; x < 600; x++) {
						for (int y = 0; y < 500; y++) {
							newImage.setRGB(x, y, tempImage.getRGB(x, y));
						}
					}
				}
				computeMap(0, 0, 600, 500);
				tempImage.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void saveToFile(File file) {
			try {
				ImageIO.write(currImage, "png", file);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		public void mouseClicked(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {
			isInApplet = true;
			mouseX = e.getX();
			mouseY = e.getY();
		}
		public void mouseExited(MouseEvent e) {
			isInApplet = false;
		}

		public void mousePressed(MouseEvent e) {
			mouseDown = true;
			startPoint = e.getPoint();
			mouseX = e.getX();
			mouseY = e.getY();
			shiftDown = e.isShiftDown();
			ctrlDown = e.isControlDown();
			if (!e.isShiftDown() && !e.isControlDown()) {
				gNewImage.setColor(particles[selectedType]);
				if (penSize > 1) {
					gNewImage.fillOval(mouseX - (penSize / 2), mouseY - (penSize / 2), penSize, penSize);
				} else {
					gNewImage.fillRect(mouseX, mouseY, 1, 1);
				}
				computeMap(mouseX - (penSize / 2), mouseY - (penSize / 2), mouseX + (penSize / 2), mouseY + (penSize / 2));
			}
		}

		public void mouseReleased(MouseEvent e) {
			mouseDown = false;
			shiftDown = e.isShiftDown();
			ctrlDown = e.isControlDown();
			if (e.isShiftDown()) {
				gNewImage.setColor(particles[selectedType]);
				gNewImage.setStroke(new BasicStroke(penSize - 1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				gNewImage.drawLine(startPoint.x, startPoint.y, e.getX(), e.getY());
				computeMap(Math.min(startPoint.x, e.getX()) - (penSize / 2), Math.min(startPoint.y, e.getY()) - (penSize / 2), Math.max(startPoint.x, e.getX()) + (penSize / 2), Math.max(startPoint.y, e.getY()) + (penSize / 2));
			} else if (e.isControlDown()) {
				gNewImage.setColor(particles[selectedType]);
				gNewImage.fillRect(startPoint.x, startPoint.y, e.getX() - startPoint.x, e.getY() - startPoint.y);
				computeMap(startPoint.x, startPoint.y, e.getX(), e.getY());
			} else {
				gNewImage.setColor(particles[selectedType]);
				if (penSize > 1) {
					gNewImage.fillOval(mouseX - (penSize / 2), mouseY - (penSize / 2), penSize, penSize);
				} else {
					gNewImage.fillRect(mouseX, mouseY, 1, 1);
				}
				computeMap(mouseX - (penSize / 2), mouseY - (penSize / 2), mouseX + (penSize / 2), mouseY + (penSize / 2));
			}
			mouseX = e.getX();
			mouseY = e.getY();
		}

		public void mouseDragged(MouseEvent e) {
			shiftDown = e.isShiftDown();
			ctrlDown = e.isControlDown();
			if (!e.isShiftDown() && !e.isControlDown()) {
				gNewImage.setColor(particles[selectedType]);
				gNewImage.setStroke(new BasicStroke(penSize - 1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				gNewImage.drawLine(mouseX, mouseY, e.getX(), e.getY());
				computeMap(Math.min(mouseX, e.getX()) - (penSize / 2), Math.min(mouseY, e.getY()) - (penSize / 2), Math.max(mouseX, e.getX()) + (penSize / 2), Math.max(mouseY, e.getY()) + (penSize / 2));
			}
			mouseX = e.getX();
			mouseY = e.getY();
		}

		public void mouseMoved(MouseEvent e) {
			shiftDown = e.isShiftDown();
			ctrlDown = e.isControlDown();
			mouseX = e.getX();
			mouseY = e.getY();
		}

		public void keyPressed(KeyEvent e) {
			switch (e.getKeyCode()) {
				case KeyEvent.VK_0:
				selectedType = 0;
				break;
				case KeyEvent.VK_1:
				selectedType = 1;
				break;
				case KeyEvent.VK_2:
				selectedType = 2;
				break;
				case KeyEvent.VK_3:
				selectedType = 3;
				break;
				case KeyEvent.VK_4:
				selectedType = 4;
				break;
				case KeyEvent.VK_C:
				clear();
				break;
				case KeyEvent.VK_B:
				boundsOn = !boundsOn;
				break;
				case KeyEvent.VK_P:
				gamePaused = !gamePaused;
				break;
			}
		}

		public void keyReleased(KeyEvent e) {}

		public void keyTyped(KeyEvent e) {}

		public void mouseWheelMoved(MouseWheelEvent e) {
			penSize = Math.min(Math.max(penSize - e.getWheelRotation(), 1), 300);
		}

		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("clear")) {
				clear();
			} else if (e.getActionCommand().equals("toggleBounds")) {
				boundsOn = !boundsOn;
			} else if (e.getActionCommand().equals("pauseGame")) {
				gamePaused = !gamePaused;
			} else if (e.getActionCommand().equals("saveImage")) {
				gamePaused = true;
				JFileChooser fc = new JFileChooser();
				fc.setDialogTitle("Save your current sand location");
				fc.removeChoosableFileFilter(fc.getFileFilter());
				fc.setFileFilter(new pngFile());
				fc.setVisible(true);
				int returnVal = fc.showSaveDialog(null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					saveFile = fc.getSelectedFile();
					saveToFile(saveFile);
				}
				gamePaused = false;
			} else if (e.getActionCommand().equals("loadImage")) {
				gamePaused = true;
				JFileChooser fc = new JFileChooser();
				fc.setDialogTitle("Load previous sand location");
				fc.removeChoosableFileFilter(fc.getFileFilter());
				fc.setFileFilter(new pngFile());
				fc.setVisible(true);
				int returnVal = fc.showOpenDialog(null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					saveFile = fc.getSelectedFile();
					loadFromFile(saveFile);
				}
				gamePaused = false;
			} else if (e.getActionCommand().equals("exitGame")) {
				System.exit(0);
			} else {
				for (Particle par : particles) {
					if (e.getActionCommand().equals("type" + par.name)) selectedType = par.type;
				}
			}
		}
	}

	class Particle extends Color {
		private static final long serialVersionUID = 1L;

		String name = "null";
		int type = -1;
		boolean gravity = false;
		boolean canMove = true;
		int slip = 0;
		int surfaceTension = 0;
		int density = 0;

		/**
		 * Used to initialize unknown particles
		 */
		public Particle(Color color) {
			super(color.getRGB());
		}

		/**
		 * Used to initialize real particles
		 * @param name
		 * @param type
		 * @param color
		 * @param grav
		 * @param slip
		 * @param surfaceTension
		 * @param density
		 */
		public Particle(String name, int type, Color color, boolean grav, boolean canmove, int slip, int surfaceTension, int density) {
			super(color.getRGB());
			this.name = name;
			this.type = type;
			this.gravity = grav;
			this.canMove = canmove;
			this.slip = slip;
			this.surfaceTension = surfaceTension;
			this.density = density;
		}

		/**
		 * Compute movements for this particle.
		 * @param x
		 * @param y
		 * @param boundsOn
		 * @param image
		 */
		HashMap<Integer, Integer> number = new HashMap<Integer, Integer>();
		public void compute(int x, int y, boolean boundsOn, BufferedImage image) {
			if (gravity) {
				int newy = y + 1;
				if (newy < 0 || newy >= 500) {
					if (!boundsOn) {
						image.setRGB(x, y, particles[0].getRGB());
						particleMap[x][y] = particles[0];
					}
					return;
				}
				int newx = x;
				if (particleMap[newx][newy].density >= density) {
					newy = y;
					if (y - 1 < 0) return;
					int temp = (particleMap[x][y - 1].slip > 0 || particleMap[Math.max(0, x - 1)][y].slip > 0 || particleMap[Math.min(x + 1, 599)][y].slip > 0 ? 1 : 0);
					int change = random(-slip - temp, slip + temp);
					int tempx = computeX(x, y, boundsOn, change);
					if (tempx == -1) {
						image.setRGB(x, y, particles[0].getRGB());
						particleMap[x][y] = particles[0];
						return;
					} else newx = tempx;
				} else if (random(0, 10) == 0) {
					int change = random(particleMap[newx][newy].slip / -5, particleMap[newx][newy].slip / 5);
					int tempx = computeX(x, y, boundsOn, change);
					if (tempx == -1) {
						image.setRGB(x, y, particles[0].getRGB());
						particleMap[x][y] = particles[0];
						return;
					} else newx = tempx;
				}
				if (particleMap[newx][newy].equals(this)) return;
				if (surfaceTension > 0 && random(1, 10) <= surfaceTension) {
					boolean left = newx - 1 >= 0;
					boolean right = newx + 1 < 600;
					boolean top = newy - 1 >= 0;
					boolean bottom = newy + 1 < 500;
					if (!((left && top && particleMap[newx - 1][newy - 1].equals(this))
						|| (top && particleMap[newx][newy - 1].equals(this))
						|| (right && top && particleMap[newx + 1][newy - 1].equals(this))
						|| (left && particleMap[newx - 1][newy].equals(this))
						|| (right && particleMap[newx + 1][newy].equals(this))
						|| (left && bottom && particleMap[newx - 1][newy + 1].equals(this))
						|| (bottom && particleMap[newx][newy + 1].equals(this))
						|| (right && bottom && particleMap[newx + 1][newy + 1].equals(this)))) return;
				}
				if (particleMap[newx][newy].canMove) {
					if (particleMap[newx][newy].gravity) particleMap[newx][newy].push(newx, newy, boundsOn, image);
					image.setRGB(x, y, particleMap[newx][newy].getRGB());
					particleMap[x][y] = particleMap[newx][newy];
					image.setRGB(newx, newy, getRGB());
					particleMap[newx][newy] = this;
				}
			}
		}

		public void computeThread(int x, int y, boolean boundsOn, BufferedImage image) {
			if (gravity) {
				int newy = y + 1;
				if (newy < 0 || newy >= 500) {
					if (!boundsOn) {
						image.setRGB(x, y, particles[0].getRGB());
						particleMap[x][y] = particles[0];
					}
					return;
				}
				if (particleMap[x][newy].density >= density) return;
				if (particleMap[x][newy].canMove) {
					image.setRGB(x, y, particleMap[x][newy].getRGB());
					particleMap[x][y] = particleMap[x][newy];
					image.setRGB(x, newy, getRGB());
					particleMap[x][newy] = this;
				}
			}
		}

		public int computeX(int x, int y, boolean boundsOn, int change) {
			int newx = x;
			if (change > 0) {
				for (int newx2 = x + 1; newx2 <= x + change; newx2++) {
					if (newx2 < 0 || newx2 >= 600) {
						if (!boundsOn) {
							return -1;
						}
						break;
					}
					if (particleMap[newx2][y].equals(this)) continue;
					if (particleMap[newx2][y].density < density && particleMap[newx2][y].canMove) {
						newx = newx2;
					} else break;
				}
			} else {
				for (int newx2 = x - 1; newx2 >= x + change; newx2--) {
					if (newx2 < 0 || newx2 >= 600) {
						if (!boundsOn) {
							return -1;
						}
						break;
					}
					if (particleMap[newx2][y].equals(this)) continue;
					if (particleMap[newx2][y].density < density && particleMap[newx2][y].canMove) {
						newx = newx2;
					} else break;
				}
			}
			return newx;
		}

		public void push(int x, int y, boolean boundsOn, BufferedImage image) {
			int bestx = x;
			int besty = -1;
			if (Math.random() >= 0.5) {
				for (int newx = Math.max(0, x - 50); newx < 600 && newx < x; newx++) {
					int tempy = getLowestY(newx, y);
					if (tempy == -1) {
						bestx = x;
						besty = -1;
						continue;
					}
					if (besty == -1 || (tempy >= besty && Math.abs(x - newx) <= Math.abs(x - bestx))) {
						besty = tempy;
						bestx = newx;
					}
				}
				for (int newx = x; newx < 600 && newx < x + 50; newx++) {
					int tempy = getLowestY(newx, y);
					if (tempy == -1) break;
					if (besty == -1 || (tempy >= besty && Math.abs(x - newx) <= Math.abs(x - bestx))) {
						besty = tempy;
						bestx = newx;
					}
				}
			} else {
				for (int newx = Math.min(599, x + 50); newx >= 0 && newx > x; newx--) {
					int tempy = getLowestY(newx, y);
					if (tempy == -1) {
						bestx = x;
						besty = -1;
						continue;
					}
					if (besty == -1 || (tempy >= besty && Math.abs(x - newx) <= Math.abs(x - bestx))) {
						besty = tempy;
						bestx = newx;
					}
				}
				for (int newx = x; newx >= 0 && newx > x - 50; newx--) {
					int tempy = getLowestY(newx, y);
					if (tempy == -1) break;
					if (besty == -1 || (tempy >= besty && Math.abs(x - newx) <= Math.abs(x - bestx))) {
						besty = tempy;
						bestx = newx;
					}
				}
			}
			if (besty == -1) return;
			if (particleMap[bestx][besty].equals(particles[0])) {
				image.setRGB(x, y, particleMap[bestx][besty].getRGB());
				particleMap[x][y] = particleMap[bestx][besty];
				image.setRGB(bestx, besty, getRGB());
				particleMap[bestx][besty] = this;
			} else if (particleMap[bestx][besty].density < density){
				particleMap[bestx][besty].push(bestx, besty, boundsOn, image);
				if (particleMap[bestx][besty].equals(particles[0])) {
					image.setRGB(x, y, particleMap[bestx][besty].getRGB());
					particleMap[x][y] = particleMap[bestx][besty];
					image.setRGB(bestx, besty, getRGB());
					particleMap[bestx][besty] = this;
				}
			}
		}

		public int getLowestY(int x, int y) {
			for (int newy = y; newy >= 0; newy--) {
				if (particleMap[x][newy].density > density) {
					return -1;
				} else if (particleMap[x][newy].density < density) return newy;
			}
			return -1;
		}
	}

	class Painter implements Runnable {
		Applet applet;
		int sleep;

		public Painter(Applet applet, int sleep) {
			this.applet = applet;
			this.sleep = sleep;
			new Thread(this) {
				{ this.setPriority(Thread.MAX_PRIORITY); }
			}.start();
		}

		public void run() {
			long timer = System.currentTimeMillis();
			long diff = 0;
			while (true) {
				diff = timer - System.currentTimeMillis();
				if (diff <= 0) {
					applet.repaint();
					diff = System.currentTimeMillis() + sleep;
				} else {
					try {
						Thread.sleep(diff);
					} catch (InterruptedException e) {
						return;
					}
				}
			}
		}
	}

	class BiasUpdater implements Runnable {
		UberSand app;
		int sleep;

		public BiasUpdater(UberSand app, int sleep) {
			this.app = app;
			this.sleep = sleep;
			new Thread(this).start();
		}

		public void run() {
			while (true) {
				app.computeBias();
				try {
					Thread.sleep(sleep);
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	}

	class Computer implements Runnable {
		UberSand app;
		int x;
		int sleep;

		public Computer(UberSand app, int x, int sleep) {
			this.app = app;
			this.x = x;
			this.sleep = sleep;
			new Thread(this).start();
		}

		public void run() {
			if (biasCache == null) app.computeBias();
			while (true) {
				for (int y = 499; y >= 0; y--) {
					for (int i = this.x; i < this.x + 20; i++) {
						int x = biasCache[i];
						if (particleMap[x][y].gravity) {
							particleMap[x][y].computeThread(x, y, boundsOn, newImage);
						}
					}
				}
				try {
					Thread.sleep(sleep);
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	}
}
