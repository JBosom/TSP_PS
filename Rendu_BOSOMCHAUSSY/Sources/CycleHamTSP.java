package tsp_ps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Map.Entry;

public class CycleHamTSP
{
	private final ArrayList<Ville> _villes;
	private ArrayList<Ville> _villesRestantes;
	private HashMap<Ville, Ville> _arretes;
	private static final Random _rand = new Random();
	private Ville _depart;
	private final int _nbVilles;
	private double _distance;
	private static final int min_thread = 5000;
	
	public CycleHamTSP(String nomFichier, boolean nonRandom)
	{
		// Initialisation des listes
		_villes = GestionFichierTSP.lectureFichier(nomFichier);
		_villesRestantes = new ArrayList<Ville>();
		_arretes = new HashMap<Ville, Ville>();
		
		_nbVilles = _villes.size();
		
		// Lancement de la fonction d'init
		if (estValide())
			this.init(nonRandom);
	}
	
	/**
	 * Fonction d'initialisation du cycle.
	 */
	public final void init(boolean nonRandom)
	{
		_villesRestantes.addAll(_villes);
		
		// On vide le cycle possiblement existant.
		_arretes.clear();
		_distance = 0;
		
		// Choix de la ville de depart aléatoirement ou non en fonction du boolean en paramètre
		if (nonRandom)
		{
			// Si le sommet n'a pas été setté.
			if (Tsp_ps.getSommetAleatoire() == -1)
			{
				int pos = _rand.nextInt(_nbVilles);
				_depart = _villes.get(pos);
				Tsp_ps.setSommetAleatoire(_depart.getNumber() - 1);
			} else
			{
				// On vérifie que le sommet est bien dans les bornes. Si ce n'est pas le cas, tant pis nous
				// settons un nouveau sommet.
				if (Tsp_ps.getSommetAleatoire() < _villes.size())
					_depart = _villes.get(Tsp_ps.getSommetAleatoire());
				else
				{
					int pos = _rand.nextInt(_nbVilles);
					_depart = _villes.get(pos);
					Tsp_ps.setSommetAleatoire(_depart.getNumber() - 1);
				}
			}
		} else
		{
			int pos = _rand.nextInt(_nbVilles);
			_depart = _villesRestantes.get(pos);
		}
		
		// On signale la ville choisie.
		System.out.println("Ville de départ : " + _depart.getNumber() + " / " + _nbVilles);
		
		// Retrait de la ville de départ des villes restantes
		_villesRestantes.remove(_depart);
	}
	
	/**
	 * Permet de savoir si le graph est valide ou non.
	 * */
	public boolean estValide()
	{
		return _nbVilles > 0;
	}
	
	/**
	 * Algorithme qui ajoute aléatoirement les villes dans le cycle
	 */
	public void algoAleatoire()
	{
		Ville v_enCours = _depart;
		Ville v_suivante;
		int pos;
		while (!_villesRestantes.isEmpty())
		{
			// On récupère une ville
			pos = _rand.nextInt(_villesRestantes.size());
			v_suivante = _villesRestantes.get(pos);
			
			// Ajout de la ville au cycle
			// _arretes.add(new Pair(v_enCours, v_suivante));
			_arretes.put(v_enCours, v_suivante);
			
			// On ajoute la distance
			_distance += v_enCours.distance(v_suivante);
			
			// On supprime la ville
			_villesRestantes.remove(v_suivante);
			v_enCours = v_suivante;
			v_suivante = null;
		}
		// _arretes.add(new Pair(v_enCours, _depart));
		_arretes.put(v_enCours, _depart);
		_distance += v_enCours.distance(_depart);
	}
	
	/**
	 * Algorithme du plus proche voisin
	 */
	public void plusProcheVoisin()
	{
		Ville v_enCours = _depart;
		Ville v_best = null;
		double d, d2;
		while (!_villesRestantes.isEmpty())
		{
			d = Double.MAX_VALUE;
			
			for (Ville v_test : _villesRestantes)
			{
				d2 = v_enCours.distanceCarre(v_test);
				if (d2 < d)
				{
					d = d2;
					v_best = v_test;
				}
			}
			
			_distance += Math.sqrt(d);
			_arretes.put(v_enCours, v_best);
			_villesRestantes.remove(v_best);
			v_enCours = v_best;
		}
		
		_arretes.put(v_enCours, _depart);
		_distance += v_enCours.distance(_depart);
	}
	
	/**
	 * Algorithme du plus proche voisin amélioré par du multi-thread. Le nombre de thread dépend du nombre de processeur
	 * présent sur la machine.
	 * 
	 * @throws InterruptedException
	 */
	public void plusProcheVoisinThreading() throws InterruptedException
	{
		Ville v_enCours = _depart;
		Ville v_best = null;
		double d, d_tmp, d_tmp2;
		int i, fourchette_val;
		Thread_ParcoursArrayVille p1, p2;
		
		if (Runtime.getRuntime().availableProcessors() > 1)
		{
			i = 0;
			
			while (!_villesRestantes.isEmpty())
			{
				i++;
				d = Double.MAX_VALUE;
				fourchette_val = _villesRestantes.size() / 2;
				
				// Pour moins de min_thread points on ne
				// fait plus de multithread.
				if (fourchette_val < min_thread)
				{
					for (Ville v_test : _villesRestantes)
					{
						d_tmp = v_enCours.distanceCarre(v_test);
						if (d_tmp < d)
						{
							v_best = v_test;
							d = d_tmp;
						}
					}
				} else
				{
					// On lance tous les threads sur
					// une plage de valeur
					// distincte.
					p1 = new Thread_ParcoursArrayVille(v_enCours, _villesRestantes, 0, fourchette_val - 1);
					p2 = new Thread_ParcoursArrayVille(v_enCours, _villesRestantes, fourchette_val, 2 * fourchette_val - 1);
					p1.start();
					p2.start();
					p1.join();
					p2.join();
					
					d_tmp = p1.getDistance();
					d_tmp2 = p2.getDistance();
					
					if (d_tmp < d_tmp2)
					{
						d = d_tmp;
						v_best = _villesRestantes.get(p1.getIndice_resultat());
					} else
					{
						d = d_tmp2;
						v_best = _villesRestantes.get(p2.getIndice_resultat());
					}
				}
				
				d = Math.sqrt(d);
				_distance += d;
				_arretes.put(v_enCours, v_best);
				_villesRestantes.remove(v_best);
				
				v_enCours = v_best;
				v_best = null;
			}
			
			_arretes.put(v_enCours, _depart);
			_distance += v_enCours.distance(_depart);
		} else
		{
			// On appelle l'algorithme simple s'il n'y a
			// qu'un seul processeur.
			plusProcheVoisin();
		}
	}
	
	/**
	 * Indique si l'on a un cycle ou non. Retourne false si la hashmap des arrête et vide, si toutes les clés ne sont pas
	 * aussi des valeurs et si la ville de départ n'est pas celle d'arrivée.
	 * 
	 * @return true si l'on a un cycle, false sinon
	 */
	public boolean estCycle()
	{
		boolean ret = true;
		
		Ville val = _depart;
		int count = 0;
		
		if (!_arretes.isEmpty())
		{
			
			if (_arretes.keySet().containsAll(_arretes.values()))
			{
				// Parcours des valeurs;
				while (count < _arretes.size())
				{
					val = _arretes.get(val);
					count++;
					// Si l'on retrouve la ville de départ
					// sans qu'on ai attend le nombre de
					// ville totale il y a un problème
					if ((val == _depart) && (count < _arretes.size()))
					{
						System.out.println("Ville de départ avant la fin du parcours du cycle");
						ret = false;
						count = _arretes.size() + 1;
					}
				}
				
				// Fin du cycle et pas la ville de départ =>
				// problème
				if (val != _depart)
				{
					System.out.println("Pas la ville de départ à la fin du cycle");
					ret = false;
				}
			} else
			{
				System.out.println("Toutes les clés ne sont pas parmis les valeurs");
				ret = false;
			}
		} else
		{
			System.out.println("Arrêtes vides");
			ret = false;
		}
		
		return ret;
	}
	
	@Override
	public String toString()
	{
		return _arretes.toString();
	}
	
	/**
	 * Renvoie la distance totale calcule lors de la construction du graphe.
	 * 
	 * @return distance totale du chemin
	 */
	public double getDistanceTotal()
	{
		return _distance;
	}
	
	/**
	 * Calcul la distance totale a partir des arretes.
	 * 
	 * @return distance totale du chemin
	 */
	public double calculDistanceTotal()
	{
		double d = 0;
		
		for (Entry<Ville, Ville> entry : _arretes.entrySet())
		{
			Ville key = entry.getKey();
			Ville value = entry.getValue();
			d += key.distance(value);
		}
		
		return d;
	}
	
	public double calculDistanceTotal(ArrayList<Ville> l)
	{
		double d = 0;
		int i;
		for (i = 0; i < l.size() - 1; i++)
		{
			d += l.get(i).distance(l.get(i + 1));
		}
		d += l.get(i).distance(l.get(0));
		
		return d;
	}
	
	/**
	 * Renvoie la ville de depart
	 * 
	 * @return Ville _depart
	 */
	public Ville get_depart()
	{
		return _depart;
	}
	
	/**
	 * Retourne le nombre de ville présentes dans l'instance
	 * 
	 * @return Nombre de villes dans l'instance
	 */
	public int get_nbVilles()
	{
		return _nbVilles;
	}
	
	/**
	 * Renvoie l'ensemble des arretes formant le chemin trouve
	 * 
	 * @return HashMap<Ville, Ville> _arretes
	 */
	public HashMap<Ville, Ville> get_arretes()
	{
		return _arretes;
	}
	
	/**
	 * Algortihme de la Plus Proche Insertion. A ete abandonne car beaucoup plus long et moins performant que le plus proche
	 * voisin.
	 */
	public void plusProcheInsertion()
	{
		Ville v_prime, v, w1, w2;
		ArrayList<Ville> _villesVisitees = new ArrayList<Ville>();
		double d, d_tmp;
		
		// On ajoute la ville la plus proche pour avoir un cycle.
		v = null;
		d = Double.MAX_VALUE;
		for (Ville vtest : _villesRestantes)
		{
			d_tmp = vtest.distanceCarre(_depart);
			if (d_tmp < d)
			{
				v = vtest;
				d = d_tmp;
			}
		}
		_arretes.put(_depart, v);
		_arretes.put(v, _depart);
		// On enleve la ville des restantes et on l'ajoute aux visitees
		_villesVisitees.add(_depart);
		_villesVisitees.add(v);
		_villesRestantes.remove(v);
		
		while (!_villesRestantes.isEmpty())
		{
			v_prime = _villesRestantes.get(0);
			v = null;
			w1 = null;
			w2 = null;
			d = Double.MAX_VALUE;
			
			// On trouve la ville la plus proche
			for (Ville v_visitee : _villesVisitees)
			{
				d_tmp = v_prime.distanceCarre(v_visitee);
				if (d_tmp < d)
				{
					v = v_visitee;
					d = d_tmp;
				}
			}
			
			// Maintenant on trouve qu'elle est la ville voisine qui
			// se liera à v_prime
			// Il ne peut y avoir que 2 possibilités.
			for (Entry<Ville, Ville> entry : _arretes.entrySet())
			{
				Ville key = entry.getKey();
				Ville value = entry.getValue();
				if (value.equals(v))
				{
					w1 = key;
					break;
				}
			}
			w2 = _arretes.get(v);
			
			// Les anciennes arretes sont automatiquement
			// remplacees.
			// Si c'est w1 la plus proche
			if (v.distance(v_prime) + v_prime.distance(w1) - v.distance(w1) < v.distance(v_prime) + v_prime.distance(w2) - v.distance(w2))
			{
				_arretes.put(w1, v_prime);
				_arretes.put(v_prime, v);
			} // Si cest w2
			else
			{
				_arretes.put(v, v_prime);
				_arretes.put(v_prime, w2);
			}
			
			// On enleve la ville des restantes et on l'ajoute aux
			// visitees
			_villesVisitees.add(v_prime);
			_villesRestantes.remove(v_prime);
		}
	}
	
	/**
	 * Inverse la liste entre les bornes i et j, i doit être inférieur à j
	 * 
	 * @param list
	 *            La liste à inverser
	 * @param i
	 *            Premier point de la liste à inverser
	 * @param j
	 *            Dernier point de la liste à inverser
	 * @return Nouvelle liste inversée
	 */
	public ArrayList<Ville> reverse(ArrayList<Ville> list, int i, int j)
	{
		ArrayList<Ville> l = new ArrayList<Ville>();
		Ville xi, xj;
		
		l.addAll(list);
		for (; i < j; i++, j--)
		{
			xi = l.get(i);
			xj = l.get(j);
			
			l.set(i, xj);
			l.set(j, xi);
		}
		return l;
	}
	
	/** Fourni une solution voisine de celle donnée en paramètre en appelant l'un des algorithmes. */
	public ArrayList<Ville> solutionVoisine(ArrayList<Ville> list, double tailleDeplacement)
	{
		/*
		 * int tmp = (int) tailleDeplacement;
		 * 
		 * if(tmp > 2) return solutionVoisineInversionPays(list, (int) tailleDeplacement); else return
		 * solutionVoisineInversionPays(list, 2);
		 */

		return solutionVoisine2Opt(list);
	}
	
	/** Inverse 2 villes qui sont voisines en choisissant aléatoirement la ville à inverser */
	public ArrayList<Ville> solutionVoisineInversionVillesVoisines(ArrayList<Ville> list)
	{
		ArrayList<Ville> res = new ArrayList<Ville>();
		int i, j, k;
		
		i = _rand.nextInt(_nbVilles);
		if (i + 1 < _nbVilles)
			j = i + 1;
		else
			j = 0;
		
		for (k = 0; k < _nbVilles; k++)
		{
			if (k == i)
			{
				res.add(list.get(j));
			} else if (k == j)
			{
				res.add(list.get(i));
			} else
			{
				res.add(list.get(k));
			}
		}
		
		return res;
	}
	
	/**
	 * Met un groupe de villes choisit aléatoirement en fin de liste. La taille du groupe a déplacer est fournie en argument.
	 */
	public ArrayList<Ville> solutionVoisineInversionPays(ArrayList<Ville> list, int tailleDeplacement)
	{
		ArrayList<Ville> res = new ArrayList<Ville>();
		int i, k;
		
		i = _rand.nextInt(_nbVilles - tailleDeplacement * 2);
		
		for (k = 0; k < _nbVilles; k++)
		{
			if (k - i < 0)
				res.add(list.get(k));
			else if ((k - i >= 0) && (k < _nbVilles - tailleDeplacement))
				res.add(list.get(tailleDeplacement + k));
			else if (k >= _nbVilles - tailleDeplacement)
			{
				res.add(list.get(i));
				i++;
			}
		}
		
		return res;
	}
	
	/** Inverse deux villes toutes deux choisient aléatoirement. */
	public ArrayList<Ville> solutionVoisineAleatoire(ArrayList<Ville> list)
	{
		ArrayList<Ville> res = new ArrayList<Ville>();
		int i, j, k;
		
		i = _rand.nextInt(_nbVilles);
		do
		{
			j = _rand.nextInt(_nbVilles);
		} while (i == j);
		
		for (k = 0; k < _nbVilles; k++)
		{
			if (k == i)
			{
				res.add(list.get(j));
			} else if (k == j)
			{
				res.add(list.get(i));
			} else
			{
				res.add(list.get(k));
			}
		}
		
		return res;
	}
	
	/** Solution voisine obtenue via un algo proche du 2-opt (1ère version) */
	public ArrayList<Ville> solutionVoisine2Opt(ArrayList<Ville> list)
	{
		ArrayList<Ville> res;
		int i, iplus1, j, jplus1, taille = list.size();
		
		do
		{
			i = _rand.nextInt(_nbVilles);
		} while (i > taille - 4);
		
		iplus1 = i + 1;
		Ville xi = list.get(i), xiplus1 = list.get(iplus1);
		
		for (j = i + 2; j < (taille - 1); ++j)
		{
			jplus1 = j + 1;
			Ville xj = list.get(j), xjplus1 = list.get(jplus1);
			
			if (xjplus1 != xi)
			{
				if (xi.distance(xiplus1) + xj.distance(xjplus1) > xi.distance(xj) + xiplus1.distance(xjplus1))
				{
					// Remplacer les arêtes (xi, xi+1) et (xj, xj+1) par (xi,
					// xj) et (xi+1, xj+1) dans H
					res = reverse(list, iplus1, j);
					// Calcule la nouvelle distance
					if (calculDistanceTotal(res) < calculDistanceTotal(list))
						return res;
				}
			}
		}
		return reverse(list, iplus1, j - 1);
	}
	
	/** Solution voisine obtenue via un algo proche du 2-opt (2de version) */
	public ArrayList<Ville> solutionVoisine2OptV2(ArrayList<Ville> list)
	{
		int i, j, taille = list.size();
		
		i = _rand.nextInt(taille - 4);
		
		if (i > 3)
		{
			j = _rand.nextInt(i - 2);
			j += taille - i + 2;
		} else
			j = taille - i - 1;
		
		return reverse(list, i + 1, j);
		
	}
	
	/** Lance une optimisation du cycle Hamiltonien via l'algorithme du Recuit Simulé */
	public void recuitSimule(double tauxLimiteAcceptation, int tempsAlloue, int nbIteration, double tauxDecrementT, boolean DEBUG)
	{
		if (DEBUG)
			System.out.println("Optimisation : Recuit Simulé(" + tauxLimiteAcceptation + ", " + tempsAlloue / 60 + ", " + nbIteration + ", " + tauxDecrementT + ")");
		
		double temp = Math.round(initRecuit(Math.round(_nbVilles / 100) + 1, nbIteration, 1000, 0.8));
		
		if (DEBUG)
			System.out.println("Température Initiale : " + temp);
		
		// Solution courante
		ArrayList<Ville> solCourante = new ArrayList<Ville>();
		double valCourante;
		
		// Initialisation de la solution courante
		Ville v = _depart;
		do
		{
			solCourante.add(v);
			v = _arretes.get(v);
		} while (v != _depart);
		valCourante = calculDistanceTotal(solCourante);
		
		// Solution voisine
		ArrayList<Ville> solVoisine;
		double valVoisine;
		
		// Stockage de la meilleure solution trouvée
		ArrayList<Ville> bestSol = new ArrayList<Ville>();
		double bestVal;
		
		bestSol.addAll(solCourante);
		bestVal = valCourante;
		
		// Utile pour le critère d'arrêt
		double tauxAcceptation;
		int compteur = 0;
		
		// Autre
		int i;
		long begin = System.currentTimeMillis(), end;
		float time = 0;
		
		// Exécution de l'algo du recuit
		do
		{
			int nbMov = 0;
			
			for (i = 0; i < nbIteration; ++i)
			{
				// Récupération d'une solution voisine
				solVoisine = solutionVoisine(solCourante, temp * 10 / _nbVilles);
				valVoisine = calculDistanceTotal(solVoisine);
				
				// Acceptation ou non de cette solution
				if (critereMetropolis((valVoisine - valCourante), temp))
				{
					solCourante = solVoisine;
					valCourante = valVoisine;
					nbMov++;
					
					// Stockage de la meilleure solution trouvée
					if (valCourante < bestVal)
					{
						bestSol = solCourante;
						bestVal = valCourante;
						// On a amélioré la solution donc on on réinitialise le compteur de non
						// amélioration
						compteur = 0;
						// System.out.println("solution amélioree ! distance : " + bestVal + "(" +
						// valCourante + ")");
					}
				}
			}
			
			// Calcul du taux d'acceptation
			tauxAcceptation = nbMov / (double) nbIteration;
			if (tauxAcceptation < tauxLimiteAcceptation)
				compteur++; // La solution ne change pas suffisamment, pour continuer
				
			// Actualise la température
			temp *= tauxDecrementT;
			
			// Calcul du temps écoulé
			end = System.currentTimeMillis();
			time = ((float) (end - begin)) / 1000f;
		} while ((compteur < 100) && (time < tempsAlloue));
		
		if (DEBUG)
		{
			end = System.currentTimeMillis();
			time = ((float) (end - begin)) / 1000f;
			System.out.println("Temps d'execution du recuit : " + time);
			System.out.println("taux : " + tauxAcceptation + ", température : " + temp + ", compteur : " + compteur);
		}
		
		// Stockage de la meilleure solution
		for (i = 0; i < (bestSol.size() - 1); i++)
		{
			_arretes.put(bestSol.get(i), bestSol.get(i + 1));
		}
		_arretes.put(bestSol.get(i), bestSol.get(0));
		
	}
	
	/**
	 * Calcule la réponse au critère de Métropolis si val < 0, on améliore la solution donc on accepte sinon, on calcule la
	 * probabilité d'acceptation par rapport à exp(-val/temp)
	 * 
	 * @param val
	 *            Valeur à considérer
	 * @param temp
	 *            Température à prendre en compte
	 * @return Booléen si l'on répond au critère de Metropolis ou non.
	 */
	public boolean critereMetropolis(double val, double temp)
	{
		if (val <= 0)
			return true;
		
		return (_rand.nextDouble() <= Math.exp(-val / temp));
	}
	
	/**
	 * Initialise la température pour le recuit
	 * 
	 * @param temp
	 *            Température initiale à tester
	 * @param iteration
	 *            Nombre d'itération pour chaque palier
	 * @param nbAcceptations
	 *            Nombre d'acceptation pour chaque palier
	 * @param taux
	 *            Taux d'acceptation que l'on veut considérer
	 * @return La température de base du recuit.
	 */
	public double initRecuit(double temp, int iteration, int nbAcceptations, double taux)
	{
		// Solution courrant
		ArrayList<Ville> solCourante = new ArrayList<Ville>();
		double valCourante;
		
		// Initialissation de la solution courante
		Ville v = _depart;
		do
		{
			solCourante.add(v);
			v = _arretes.get(v);
		} while (v != _depart);
		
		valCourante = calculDistanceTotal(solCourante);
		
		// Solution voisine
		ArrayList<Ville> solVoisine;
		double valVoisine;
		
		// Autre
		boolean continuer = true;
		int nbMov, i;
		
		// Exécution de l'algo du recuit
		while (continuer)
		{
			nbMov = 0;
			i = 0;
			
			while ((i < iteration) && (nbMov < nbAcceptations))
			{
				// Récupération d'une solution voisine
				solVoisine = solutionVoisine(solCourante, temp * 10 / _nbVilles);
				valVoisine = calculDistanceTotal(solVoisine);
				
				// Acceptation ou non de cette solution
				if (critereMetropolis((valVoisine - valCourante), temp))
				{
					solCourante = solVoisine;
					valCourante = valVoisine;
					nbMov++;
				}
				i++;
			}
			
			// Calcul du taux d'acceptation pour augmenter ou non la température
			if ((nbMov / (double) i) < taux)
			{
				temp *= 2;
			} else
				continuer = false;
		}
		return temp;
	}
	
}
