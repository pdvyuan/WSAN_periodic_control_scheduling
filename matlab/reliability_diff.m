%two different kinds of scheduling.
%1: wait at the GW. 
%2: no-wait at the GW. 
%suppose the link quality of all subpath is equal to p.
function reliability_diff()
    %k: number of paths.    
    for k = 2:10
        p = 0:0.01:1;
        reliability1 = (1-(1-p).^k).^2;
        reliability2 = 1-(1-p.^2).^k;
        delta = reliability1 - reliability2;
        plot(p, delta);
        hold on;
    end
    xlabel('path reliability \it{r}');
    ylabel('difference in flow reliability');
    p=0.19;
    k=10;
    text(p, (1-(1-p).^k).^2-1+(1-p.^2).^k,'\leftarrow n = 10',...
     'HorizontalAlignment','left', 'FontSize', 15)
    k=2;
    text(p, (1-(1-p).^k).^2-1+(1-p.^2).^k,'\leftarrow n = 2',...
     'HorizontalAlignment','left', 'FontSize', 15)
    set(gca,'fontsize', 15);
end