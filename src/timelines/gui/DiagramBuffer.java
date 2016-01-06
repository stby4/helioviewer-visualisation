package timelines.gui;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by Tobi on 05.12.2015.
 */
public class DiagramBuffer {
    private ImageLoader imageLoader;
    private TreeMap<Long, Diagram> treeMap = new TreeMap<Long, Diagram>();
    private int tileCount;
    private int currentCount = 0;

    private ArrayList<Diagram> list = new ArrayList<>();//TODO:remove


    public DiagramBuffer(ImageLoader imageLoader, int tileCount){
        this.imageLoader = imageLoader;
        this.tileCount = tileCount;
    }

    public synchronized void addToDiagramBuffer(Diagram diagram) throws IOException{
        Long date = diagram.getStartDate().getTime();
        treeMap.put(date, diagram);
        list.add(diagram);//TODO:remove

        this.currentCount++;
        reportIfFull();
    }

    private void reportIfFull(){
        if (this.currentCount == this.tileCount){
            try {//TODO:remove
                for (int i = 0; i<list.size();i++){
                    BufferedImage bi = list.get(i).getBufferedImage();
                    File outputfile = new File("C:/Users/Tobi/Pictures/saved"+i+".png");
                    ImageIO.write(bi, "png", outputfile);
                }
            } catch (IOException e) {//TODO:remove

            }//TODO:remove
            this.imageLoader.processDiagramBuffer();
        }
    }

    public TreeMap<Long, Diagram> getMap(){
        return this.treeMap;
    }

    public BufferedImage[] getSortedBufferedImageArray(){
        BufferedImage[] buffImgArr = new BufferedImage[this.tileCount];
        int i = 0;
        for(Long key : this.treeMap.keySet())
        {
            buffImgArr[i] = this.treeMap.get(key).getBufferedImage();
            i++;
        }
        return buffImgArr;
    }

}
