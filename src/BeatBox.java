
import java.awt.*;
import javax.swing.*;
import javax.sound.midi.*;
import java.io.*;
import java.util.*;
import java.awt.event.*;

/**
 * Основной класс с реализацией графического интерфейса и логики.
 */

public class BeatBox {
    JPanel mainPanel;
    ArrayList<JCheckBox> checkboxList; // лист для хранения флажков
    Sequencer sequencer;
    Sequence sequence;
    Track track;
    JFrame theFrame;
    JFileChooser fileChooser = new JFileChooser();


    /*
    Ниже перечеслияем названия инструментов в виде строк.
    Для создания меток в пользовательском интерфейсе (на каждый ряд)
     */
    String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat",
            "Open Hi-Hat", "Acoustic Snare", "Crash Cymbal", "Hand Clap",
            "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga",
            "Cowbell", "Vibraslap", "Low-mid Tom", "High Agogo",
            "Open Hi Conga"};

    /*
    Эти числа представляют собой фактические барабанные клавиши. Канал балабана - это что то вроде фортепиано,
     только каждая клавишана нём - отдельный барабан.
     */
    int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};

    /*
    метод по созданию графического интерфейса и наполнению его кнопками и флажками
     */
    public void buildGUI() {
        theFrame = new JFrame("Cyber BeatBox");
        theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        background.setBorder(BorderFactory
                .createEmptyBorder(10, 10, 10, 10));
        //пустая граница ↑ позволяет создать поля между краями панели и местом размещения компанентов

        checkboxList = new ArrayList<>();
        Box buttonBox = new Box(BoxLayout.Y_AXIS);

        JButton start = new JButton("Воспроизвести");
        start.addActionListener(new MyStartListener());
        buttonBox.add(start);

        JButton stop = new JButton("Остановить");
        stop.addActionListener(new MyStopListener());
        buttonBox.add(stop);

        JButton upTempo = new JButton("Увеличить темп");
        upTempo.addActionListener(new MyUpTempoListener());
        buttonBox.add(upTempo);

        JButton downTempo = new JButton("Замедлить Down");
        downTempo.addActionListener(new MyDownTempoListener());
        buttonBox.add(downTempo);

        JButton clean = new JButton("Очистить все флажки");
        clean.addActionListener(new MyCleanListener());
        buttonBox.add(clean);

        JButton saveIt = new JButton("Сохранить");
        saveIt.addActionListener(new MySendListener());
        buttonBox.add(saveIt);

        JButton restore = new JButton("Восстановить сохраненный выбор");
        restore.addActionListener(new MyReadInListener());
        buttonBox.add(restore);

        Box nameBox = new Box(BoxLayout.Y_AXIS);
        for (int i = 0; i < 16; i++) {
            nameBox.add(new Label(instrumentNames[i]));
        }

        background.add(BorderLayout.EAST, buttonBox);
        background.add(BorderLayout.WEST, nameBox);

        theFrame.getContentPane().add(background);

        GridLayout grid = new GridLayout(16, 16);
        grid.setVgap(1);
        grid.setHgap(2);
        mainPanel = new JPanel(grid);
        background.add(BorderLayout.CENTER, mainPanel);

        //создаем флажки и присваеваем им значения false
        for (int i = 0; i < 256; i++) {
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            checkboxList.add(c); // добавляем в массив
            mainPanel.add(c);// добавляем на панель
        }

        setUpMidi();

        theFrame.setBounds(50, 50, 300, 300);
        theFrame.pack();
        theFrame.setVisible(true);
    }


    /*
    Метод для получения синтезатора, секвентора и дорожки
     */
    public void setUpMidi() {
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ, 4);
            track = sequence.createTrack();
            sequencer.setTempoInBPM(120);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
    Основной метод, который преобразует состаяния флажков в MIDI-события
     */
    public void buildTrackAndStart() {
        //массив из 16 элементов для каждого из инструментов на все 16 тактов
        int[] trackList = new int[16];

        //избавляемся от старой дорожки и создаем новую
        sequence.deleteTrack(track);
        track = sequence.createTrack();

        for (int i = 0; i < 16; i++) { // для каждого из инструметов (Басс, Конго, Hi-hat..)

            int key = instruments[i]; // Задаем клавишу, которая представляет инструмент

            for (int j = 0; j < 16; j++) { // для каждого такта текущего ряда
                JCheckBox jc = (JCheckBox) checkboxList.get(j + (16 * i));
                if (jc.isSelected()) {     // установлен ли флажек?
                    trackList[j] = key;     // если да - то помещаем значение клавиши в текущую ячейку массива
                } else {
                    trackList[j] = 0;      // если нет - инструмент не играет потому что присвоили 0
                }
            }

            //для этого инструмента и для всех 16 тактов создаем события и добавляем их на дорожку
            makeTracks(trackList);

            track.add(makeEvent(176, 1, 127, 0, 16));
            /*
             Пять чисел, переданных в качестве аргументов представляют различные параметры события MIDI
             (управление контроля,номер музыканта, уровень громкости или Номер программы Например,
             программы от 1 до 8 могут представлять различные звуки фортепиано,
             а программы от 17 до 24 могут представлять различные звуки гитары.  , позиция такта и т.д.)
             */
        }
        track.add(makeEvent(192, 9, 1, 0, 15));
        // пробуем проигрывать мелодию:
        try {
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY); // непрерывное повторение цикла
            sequencer.start();
            sequencer.setTempoInBPM(120);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /*
    Ниже блок внутренних классов - слушателей для кнопок(Стракт, стоп, быстрее или медленее)
     */
    public class MyStartListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            buildTrackAndStart();
        }
    }

    public class MyStopListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            sequencer.stop();
        }
    }

    public class MyUpTempoListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempoFactor * 1.03));
        }
    }

    public class MyDownTempoListener implements ActionListener {

        public void actionPerformed(ActionEvent a) {
            float tempoFactor = sequencer.getTempoFactor();
            sequencer.setTempoFactor((float) (tempoFactor * .97));
        }
    }

    public class MyCleanListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            for (JCheckBox check : checkboxList) {
                check.setSelected(false);
            }
        }
    }

    public class MySendListener implements ActionListener {
        public void actionPerformed(ActionEvent a) {
            fileChooser.setCurrentDirectory(new File("src/save"));//- чтобы по умолчанию открывался пакет "src.save" в диалоговом окне.
            int result = fileChooser.showSaveDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                // сохраняем данные в выбранный файл

                // создаем булев массив для хранения состояния каждого флажка
                boolean[] checkboxState = new boolean[256];

                for (int i = 0; i < 256; i++) {
                    JCheckBox check = checkboxList.get(i);
                    if (check.isSelected()) {
                        checkboxState[i] = true;
                    }
                }

                try { //сораняем массив флажков в файл "Checkbox.ser"
                    FileOutputStream fileStream = new FileOutputStream(
                            file);
                    ObjectOutputStream os = new ObjectOutputStream(fileStream);
                    os.writeObject(checkboxState);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            }
        }
    }

    public class MyReadInListener implements ActionListener {  // new - restore
        public void actionPerformed(ActionEvent a) {
            fileChooser.setCurrentDirectory(new File("src/save"));
            int result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                // сохраняем данные в выбранный файл


                boolean[] checkboxState = null;
                try {
                    FileInputStream fileIn = new FileInputStream(
                            file);
                    ObjectInputStream is = new ObjectInputStream(fileIn);
                    checkboxState = (boolean[]) is.readObject();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                // Теперь восстанавливаем состояние каждого флажка в ArrayList, содержащий объекты JCheckBox
                for (int i = 0; i < 256; i++) {
                    JCheckBox check = checkboxList.get(i);
                    if (checkboxState[i]) {
                        check.setSelected(true);
                    } else {
                        check.setSelected(false);
                    }
                }

                sequencer.stop();
                buildTrackAndStart();
            }
        }
    }

    /*
     Метод создает дорожку событий для одного инструмента за каждый проход цикла для всех 16 тактов.
     */
    public void makeTracks(int[] list) {

        for (int i = 0; i < 16; i++) {
            int key = list[i];

            if (key != 0) {
                track.add(makeEvent(144/*тип сообщения*/, 9/*номер мыузыканта*/, key/*инструмент*/, 100/*скорость и сила нажатия клавиши*/, i));
                track.add(makeEvent(128, 9, key, 100, i + 1));
            }
        }
    }


    /*
    метод по созданию звукового события
     */
    public MidiEvent makeEvent(int comd, int chan, int one, int two, int tick) {
        MidiEvent event = null;
        try {
            ShortMessage a = new ShortMessage();
            a.setMessage(comd, chan, one, two);
            event = new MidiEvent(a, tick);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return event;
    }

}


